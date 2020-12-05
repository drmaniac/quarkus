package io.quarkus.elasticsearch.panache.deployment;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.elasticsearch.panache.runtime.ElasticsearchOperations;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.MetamodelInfo;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;

public class PanacheElasticsearchEntityEnhancer
        extends PanacheEntityEnhancer<MetamodelInfo<EntityModel<EntityField>>> {

    public final static String ELASTICSEARCH_OPERATIONS_NAME = ElasticsearchOperations.class.getName();
    public final static String ELASTICSEARCH_OPERATIONS_BINARY_NAME = ELASTICSEARCH_OPERATIONS_NAME.replace('.', '/');

    final Map<String, EntityModel> entities = new HashMap<>();

    public PanacheElasticsearchEntityEnhancer(IndexView index, List<PanacheMethodCustomizer> methodCustomizers) {
        super(index, methodCustomizers);
        modelInfo = new MetamodelInfo<>();
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void collectFields(ClassInfo classInfo) {
        EntityModel<EntityField> entityModel = new EntityModel<>(classInfo);
        for (FieldInfo fieldInfo : classInfo.fields()) {
            String name = fieldInfo.name();
            if (Modifier.isPublic(fieldInfo.flags())) {
                entityModel.addField(new EntityField(name, DescriptorUtils.typeToString(fieldInfo.type())));
            }
        }
        modelInfo.addEntityModel(entityModel);
    }

}
