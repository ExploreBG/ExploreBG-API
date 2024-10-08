package bg.exploreBG.serializer;

import bg.exploreBG.model.enums.GenderEnum;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GenderEnumSerializer extends JsonSerializer<GenderEnum> {
    @Override
    public void serialize(
            GenderEnum value,
            JsonGenerator gen,
            SerializerProvider serializers
    ) throws IOException {
        gen.writeString(value.getValue());
    }
}
