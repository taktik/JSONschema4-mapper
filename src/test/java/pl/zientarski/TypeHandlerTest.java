package pl.zientarski;

import com.google.common.collect.Lists;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import pl.zientarski.typehandler.TypeHandler;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TypeHandlerTest {
    private SchemaMapper mapper;

    class SomeType {

    }

    class Hamster {
        private SomeType someField;

        public SomeType getSomeField() {
            return someField;
        }
    }

    @Before
    public void before() {
        mapper = new SchemaMapper();
    }

    @Test
    public void typeHandlerTest() throws Exception {
        //given
        mapper.addTypeHandler(new TypeHandler() {
            @Override
            public boolean accepts(final Type type) {
                return type.equals(Hamster.class);
            }

            @Override
            public JSONObject process(final TypeDescription typeDescription, final MapperContext mapperContext) {
                final JSONObject result = new JSONObject();
                result.put("pika-pika", "Pikachu");
                return result;
            }
        });

        //when
        final JSONObject schema = mapper.toJsonSchema4(Hamster.class);

        //then
        assertTrue(schema.has("pika-pika"));
        assertThat(schema.getString("pika-pika"), equalTo("Pikachu"));
    }

    @Test
    public void typeHandlerDependenciesTest() throws Exception {
        //given
        mapper.addTypeHandler(new TypeHandler() {
            @Override
            public boolean accepts(final Type type) {
                return type.equals(Hamster.class);
            }

            @Override
            public JSONObject process(final TypeDescription typeDescription, final MapperContext mapperContext) {
                final JSONObject result = new JSONObject();
                final Set<PropertyDescription> properties = typeDescription.getProperties();
                properties
                        .stream()
                        .filter(PropertyDescription::hasField)
                        .filter(PropertyDescription::hasGetter)
                        .forEach(dep -> mapperContext.addDependency(dep.getType()));

                return result;
            }
        });

        //when
        mapper.toJsonSchema4(Hamster.class);

        //then
        final Iterator<Type> dependenciesIterator = mapper.getDependencies();
        final ArrayList<Type> dependencies = Lists.newArrayList(dependenciesIterator);
        assertThat(dependencies, hasSize(1));
        assertThat(dependencies, hasItem(SomeType.class));
    }

    @Test
    public void typeHandlerNoDependenciesTest() throws Exception {
        //given
        mapper.addTypeHandler(new TypeHandler() {
            @Override
            public boolean accepts(final Type type) {
                return type.equals(Hamster.class);
            }

            @Override
            public JSONObject process(final TypeDescription typeDescription, final MapperContext mapperContext) {
                return new JSONObject();
            }
        });

        //when
        mapper.toJsonSchema4(Hamster.class);

        //then
        final Iterator<Type> dependenciesIterator = mapper.getDependencies();
        final ArrayList<Type> dependencies = Lists.newArrayList(dependenciesIterator);
        assertThat(dependencies, hasSize(0));
    }
}
