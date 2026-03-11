package com.example.demo.chat.application;

import com.example.demo.chat.api.dto.PlaceExtractResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

// [음성채팅 기능]
@Service
@RequiredArgsConstructor
public class PlaceExtractService {

    private final OpenAiChatModel chatModel;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${app.openai.extract.model:gpt-4o-mini}")
    private String extractModel;

    @Value("${app.openai.extract.temperature:0}")
    private double extractTemperature;

    //  Structured Outputs(JSON Schema): 분류 제거 → places만
    private static final String SCHEMA = """
        {
          "type": "object",
          "properties": {
            "places": { "type": "array", "items": { "type": "string" } }
          },
          "required": ["places"],
          "additionalProperties": false
        }
        """;

    public PlaceExtractResponse extractPlaces(List<String> chatLines) {
        if (chatLines == null || chatLines.isEmpty()) {
            PlaceExtractResponse empty = new PlaceExtractResponse();
            empty.setPlaces(List.of());
            return empty;
        }

        var system = new SystemMessage("""
            너는 "여행 채팅 로그"에서 "장소 이름"만 추출하는 엔진이다.

            출력 규칙:
            - 반드시 JSON만 출력한다.
            - 아래 스키마를 정확히 따른다.
            - places 배열에는 "장소 이름 문자열"만 담는다.
            - 중복 이름 제거.
            - 사람 이름/감탄사/일반 단어(예: '내일', '점심', '렌트카')는 제외.
            - 숙박/맛집/관광지로 분류하지 말고, 등장한 장소를 모두 places에 누적한다.
            """);

        var user = new UserMessage("""
            아래 채팅에서 장소 이름만 추출해줘.

            채팅:
            """ + String.join("\n", chatLines));

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(extractModel)
                .temperature(extractTemperature)
                .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, SCHEMA)) // 스키마만 변경됨
                .build();

        Prompt prompt = new Prompt(List.of(system, user), options);

        try {
            System.out.println("[EXTRACT] model=[" + extractModel + "], temp=" + extractTemperature);

            var res = chatModel.call(prompt);
            String json = res.getResult().getOutput().getText();
            return om.readValue(json, PlaceExtractResponse.class);
        } catch (Exception e) {
            PlaceExtractResponse empty = new PlaceExtractResponse();
            empty.setPlaces(List.of());
            return empty;
        }
    }
}