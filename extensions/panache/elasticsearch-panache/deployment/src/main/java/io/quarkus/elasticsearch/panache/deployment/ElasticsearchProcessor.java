package io.quarkus.elasticsearch.panache.deployment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.elasticsearch.panache.PanacheElasticsearchEntity;
import io.quarkus.elasticsearch.panache.PanacheElasticsearchEntityBase;
import io.quarkus.elasticsearch.panache.runtime.ElasticsearchConfig;
import io.quarkus.elasticsearch.panache.runtime.ElasticsearchProducer;
import io.quarkus.elasticsearch.panache.runtime.ElasticsearchRecorder;
import io.quarkus.panache.common.deployment.PanacheEntityClassesBuildItem;
import io.quarkus.panache.common.deployment.PanacheFieldAccessEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizerBuildItem;

public class ElasticsearchProcessor {

    public static final String ELASTICSEARCH_PANACHE = "io.quarkus.elasticsearch.panache";

    static final DotName DOTNAME_PANACHE_ENTITY_BASE = DotName.createSimple(PanacheElasticsearchEntityBase.class.getName());
    private static final DotName DOTNAME_PANACHE_ENTITY = DotName.createSimple(PanacheElasticsearchEntity.class.getName());

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(ELASTICSEARCH_PANACHE);
    }

    @BuildStep
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem("elasticsearch-panache");
    }

    @BuildStep
    void build(CombinedIndexBuildItem index,
            ApplicationIndexBuildItem applicationIndex,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<PanacheEntityClassesBuildItem> entityClasses,
            List<PanacheMethodCustomizerBuildItem> methodCustomizersBuildItems) throws Exception {

        List<PanacheMethodCustomizer> methodCustomizers = methodCustomizersBuildItems.stream()
                .map(bi -> bi.getMethodCustomizer()).collect(Collectors.toList());

        PanacheElasticsearchEntityEnhancer modelEnhancer = new PanacheElasticsearchEntityEnhancer(index.getIndex(),
                methodCustomizers);
        Set<String> modelClasses = new HashSet<>();
        // Note that we do this in two passes because for some reason Jandex does not give us subtypes
        // of PanacheMongoEntity if we ask for subtypes of PanacheMongoEntityBase
        for (ClassInfo classInfo : index.getIndex()
                .getAllKnownSubclasses(DOTNAME_PANACHE_ENTITY_BASE)) {
            if (classInfo.name().equals(DOTNAME_PANACHE_ENTITY))
                continue;
            if (modelClasses.add(classInfo.name().toString()))
                modelEnhancer.collectFields(classInfo);
        }
        for (ClassInfo classInfo : index.getIndex().getAllKnownSubclasses(DOTNAME_PANACHE_ENTITY)) {
            if (modelClasses.add(classInfo.name().toString()))
                modelEnhancer.collectFields(classInfo);
        }

        // iterate over all the entity classes
        for (String modelClass : modelClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(modelClass, modelEnhancer));

            // register for reflection entity classes
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, modelClass));

            // Register for building the property mapping cache
            // propertyMappingClass.produce(new PropertyMappingClassBuildStep(modelClass));
        }
        if (!modelClasses.isEmpty()) {
            entityClasses.produce(new PanacheEntityClassesBuildItem(modelClasses));
        }

        if (!modelEnhancer.entities.isEmpty()) {
            PanacheFieldAccessEnhancer panacheFieldAccessEnhancer = new PanacheFieldAccessEnhancer(
                    modelEnhancer.getModelInfo());
            for (ClassInfo classInfo : applicationIndex.getIndex().getKnownClasses()) {
                String className = classInfo.name().toString();
                if (!modelClasses.contains(className)) {
                    transformers
                            .produce(new BytecodeTransformerBuildItem(className, panacheFieldAccessEnhancer));
                }
            }
        }
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(loadsApplicationClasses = true)
    public void build(ElasticsearchRecorder recorder,
            BuildProducer<AdditionalBeanBuildItem> additionalBean) {
        AdditionalBeanBuildItem unremovableProducer = AdditionalBeanBuildItem.unremovableOf(ElasticsearchProducer.class);
        additionalBean.produce(unremovableProducer);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    public void elasticsearchBuildStep(ElasticsearchRecorder recorder,
            ElasticsearchConfig elasticsearchConfig,
            BeanContainerBuildItem beanContainer) {
        recorder.setElasticsearchConfig(elasticsearchConfig, beanContainer.getValue());
        recorder.doStartActions(elasticsearchConfig, beanContainer.getValue());
    }
}
