package io.quarkus.elasticsearch.panache.deployment;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

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
        super(index, ElasticsearchProcessor.DOTNAME_PANACHE_ENTITY_BASE, methodCustomizers);
        modelInfo = new MetamodelInfo<>();
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new PanacheElasticsearchEntityClassVisitor(className, outputClassVisitor, modelInfo, panacheEntityBaseClassInfo,
                indexView.getClassByName(DotName.createSimple(className)), methodCustomizers);
    }

    static class PanacheElasticsearchEntityClassVisitor extends PanacheEntityClassVisitor<EntityField> {

        public PanacheElasticsearchEntityClassVisitor(String className, ClassVisitor outputClassVisitor,
                MetamodelInfo<EntityModel<EntityField>> modelInfo, ClassInfo panacheEntityBaseClassInfo,
                ClassInfo entityInfo, List<PanacheMethodCustomizer> methodCustomizers) {
            super(className, outputClassVisitor, modelInfo, panacheEntityBaseClassInfo, entityInfo, methodCustomizers);
        }

        @Override
        protected void injectModel(MethodVisitor mv) {
            mv.visitLdcInsn(thisClass);
        }

        @Override
        protected String getModelDescriptor() {
            return "Ljava/lang/Class;";
        }

        @Override
        protected String getPanacheOperationsBinaryName() {
            return ELASTICSEARCH_OPERATIONS_BINARY_NAME;
        }

        @Override
        protected void generateAccessorSetField(MethodVisitor mv, EntityField field) {
            mv.visitFieldInsn(Opcodes.PUTFIELD, thisClass.getInternalName(), field.name, field.descriptor);
        }

        @Override
        protected void generateAccessorGetField(MethodVisitor mv, EntityField field) {
            mv.visitFieldInsn(Opcodes.GETFIELD, thisClass.getInternalName(), field.name, field.descriptor);
        }
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
