package kz.home.RelaySmartSystems;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.CRC32;

public class Utils {
    public static String removeFieldsJSON(String json, String... fieldsToRemove) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = null;
        String cleanedJson = null;
        try {
            root = mapper.readTree(json);
            removeFieldsRecursive(root, fieldsToRemove);
            cleanedJson = mapper.writer().writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return cleanedJson;
    }

    private static void removeFieldsRecursive(JsonNode node, String... fieldsToRemove) {
        if (node.isObject()) {
            ObjectNode objNode = (ObjectNode) node;
            for (String field : fieldsToRemove) {
                objNode.remove(field);
            }
            Iterator<Map.Entry<String, JsonNode>> iter = objNode.fields();
            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> entry = iter.next();
                removeFieldsRecursive(entry.getValue(), fieldsToRemove);
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                removeFieldsRecursive(item, fieldsToRemove);
            }
        }
    }

    public static String getJson(Object object) {
        String json;
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        try {
            json = objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return json;
    }

    public static long getCRC(Object object) {
        CRC32 crc = new CRC32();
        crc.update(getJson(object).getBytes(StandardCharsets.UTF_8));
        return crc.getValue();
    }

    public static String getShortId(String uuidStr, int length) throws Exception {
        // 1. Получаем хеш SHA-256 от строки UUID
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(uuidStr.getBytes(StandardCharsets.UTF_8));

        // 2. Кодируем в Base64 (URL safe формат, без лишних символов)
        String base64 = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        // 3. Берем первые N символов
        return base64.substring(0, length);
    }

    public static int crc16(byte[] bytes) {
        int crc = 0xFFFF;
        for (byte b : bytes) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        return crc;
    }

    public static int crc16(byte[] bytes, int length) {
        int crc = 0xFFFF;
        for (int j = 0; j < length; j++) {
            crc ^= (bytes[j] & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        return crc;
    }
}
