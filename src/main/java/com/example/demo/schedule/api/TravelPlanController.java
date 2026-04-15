package com.example.demo.schedule.api;

import com.example.demo.chat.api.dto.TravelChatRequest;
import com.example.demo.chat.application.ChatMessageService;
import com.example.demo.chat.application.RoomService;
import com.example.demo.map.application.TourPlaceService;
import com.example.demo.schedule.application.TravelPlanService;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.text.MessageFormat;


@Controller
public class TravelPlanController {

    @Autowired
    private OpenAiChatModel chatModel;

    @Autowired private RoomService roomService;
    @Autowired private ChatMessageService chatMessageService;
    @Autowired private TourPlaceService tourPlaceService;
    @Autowired private TravelPlanService travelPlanService;

    @GetMapping("/travel")
    public String getTravel() {
        return "travel-request";
    }


    @PostMapping("/travel")
    @ResponseBody //  명시적으로 Body에 데이터를 쓴다는 의미
    public ResponseEntity<String> postTravel(@RequestBody TravelChatRequest request) {

        String roomId = (request.getRoomId() == null || request.getRoomId().isBlank())
                ? "default" : request.getRoomId();

        roomService.getOrCreate(roomId);
        roomService.touch(roomId);


        String updatedAt = OffsetDateTime.now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        //  선택된 명소 정보 포맷팅 (선택 안 했으면 '없음')
        String selectedPlaceInfo = "없음";
        if (request.getSelectedPlaceName() != null && !request.getSelectedPlaceName().isEmpty()) {
            selectedPlaceInfo = MessageFormat.format(
                    "명소명: {0} (위도: {1}, 경도: {2})",
                    request.getSelectedPlaceName(),
                    request.getSelectedLat(),
                    request.getSelectedLng()
            );
        }

        // 선택 맛집
        String selectedRestaurantInfo = "없음";
        if (request.getSelectedRestaurantName() != null && !request.getSelectedRestaurantName().isEmpty()) {
            selectedRestaurantInfo = MessageFormat.format(
                    "맛집명: {0} (위도: {1}, 경도: {2})",
                    request.getSelectedRestaurantName(),
                    request.getSelectedRestaurantLat(),
                    request.getSelectedRestaurantLng()
            );
        }

        //  선택 숙박
        String selectedStayInfo = "없음";
        if (request.getSelectedStayName() != null && !request.getSelectedStayName().isEmpty()) {
            selectedStayInfo = MessageFormat.format(
                    "숙소명: {0} (위도: {1}, 경도: {2})",
                    request.getSelectedStayName(),
                    request.getSelectedStayLat(),
                    request.getSelectedStayLng()
            );
        }

        // DB에서 채팅 로그 구성
        String chatLogFromDb = chatMessageService.buildChatLog(roomId);

        // DB에서 TourAPI 후보 목록 구성 (title/lat/lng만)
        String candidates = tourPlaceService.buildCandidateList(roomId, 300);

        var systemMessage = new SystemMessage("""
                너는 여행 일정 플래너야.
                사용자가 제공한 "여행 관련 채팅 로그"와 "TourAPI로부터 불러온 장소 후보 목록(장소명/위도/경도)"만을 사용해서
                아래 JSON 형식의 여행 일정을 생성해. 쌓여진 채팅내역 중 최신 채팅들을 우선적으로 반영해.
                
                출력 형식 조건(중요):
                - 무조건 유효한 JSON만 출력해. (설명 문장, 마크다운, 자연어 주석 금지)
                - 모든 키는 쌍따옴표(")로 감싸.
                - 주석(//, /* */) 사용 금지.
                - JSON 바깥에 어떤 텍스트도 추가하지 마.
                
                절대 규칙 (매우 중요)
                1) 집결 장소(여행 시작점)
                -채팅 로그에서 "집결", "만나자", "어디서 보자", "모이자" 등으로
                 여행 시작을 위해 함께 모이기로 한 장소가 있다면,
                 그 장소를 여행계획json의 첫 번째 요소로 둬.
                
                2) 장소 데이터 소스
                - 일정에 등장하는 장소(place, lat, lng)는 최대한 "TourAPI 장소 후보 목록"에 포함된 장소로해.
                - 제공된 장소이름,위도,경도가 아니여도 items에 넣을께 없으면 적절하게 생성해서 넣어. 공항이나 지하철역 언급되면 정확하게 넣어줘.
                
                
                3) 교통/이동/액션 정보는 전부 제외
                - MOVE, ACTION 같은 타입을 만들지 마.
                - 버스/지하철/렌트카/거리/소요시간/요금 등 교통 관련 정보를 출력에 포함하지 마.
                - 오직 "방문/식사/숙박/휴식" 같은 이벤트만 일정으로 출력해.
                
                4) 일정이 비는 구간은 후보 목록에서 채우기
                - 채팅 로그만으로 일정 흐름이 끊기거나 중간 시간이 많이 비면,
                  "TourAPI 장소 후보 목록"에서 동선/테마가 자연스럽게 이어지도록 적절한 장소를 골라 채워 넣어.
                - 단, 후보 목록에 없는 장소는 절대 추가하지 마.
                
                5) 사용자 선택 필수 반영
                - "사용자 선택 필수 관광지/맛집/숙소"가 "없음"이 아니라면,
                  반드시 items에 포함시켜야 한다.
                - 이때 place/lat/lng는 사용자가 제공한 값을 그대로 사용한다.
                - 맛집은 식사 시간대에, 숙소는 저녁/야간 시간대에 배치해.
                
                6) 시간 구성 규칙
                - 채팅 로그의 날짜/시간/방문할 곳 힌트를 반영해서 합리적인 시간표를 구성해.
                - 채팅에 언급된 장소,숙소,음식점들을 무조건 다 가야할 필요는 없어.
                - 장소,식당,숙소 등 1곳을 위해서 먼 거리를 이동하지 않도록 위도,경도를 고려해서 여행계획을 세워줘.
                - 이동경로를 효율적으로 짜줘.
                - 너무 빡빡하지 않게 (관광/식사/카페/휴식/숙박 등)이 적절히 섞이도록 만든다.
                - 밤샘 금지: 새벽 1시~5시 사이에는 가능한 한 활동을 넣지 말고 숙소/휴식으로 간주해.
                
                7) 추가 규칙
                - 사용자들이 언급한 식당들이 모두 들어갈 필요는 없어. 아침식당, 점심식당, 저녁식당을 모두 억지로 넣을 필요 없어. 
                위도,경도로 계산했을때 후보목록의 식당들이 관광명소나 숙박업소 등에서 너무 멀면 과감히 식사를 스킵해도 좋고 가까운 식당 있으면 넣어줘.
                
                
                
                최종 출력 JSON 스키마
                {
                  "roomId": string,
                  "updatedAt": string (ISO-8601, 예: 2025-10-23T20:40:00+09:00),
                  "items": [
                    {
                      "month": number,
                      "day": number,
                      "hour": number,
                      "minute": number,
                      "place": string,
                      "lat": number,
                      "lng": number,
                      "imageUrl": string, 
                      "memo": string
                    }
                  ]
                }
                
                items 작성 규칙:
                - 모든 항목은 동일한 구조만 사용한다. (type 같은 분기 금지)
                - items는 시간 순서대로 정렬한다.
                - 제공된 장소 후보의 image 값이 있다면 imageUrl에 그대로 넣어줘.(없으면 빈 문자열 "") // [사진추가]
                - memo는 채팅 로그 기반으로 자연스럽게 풍성하게 작성한다.
                  (예: "사진 많이 찍기", "대표 메뉴 먹기", "체크인 후 휴식", "근처 산책" 등)
                - 단, memo에 교통수단/거리/요금/소요시간 같은 교통 정보는 넣지 마.
                
                출력은 반드시 JSON 하나만.
                """);

        //  유저메시지에 3종 선택값 주입
        var userMessage = new UserMessage(MessageFormat.format("""
                ### 입력 정보
                - roomId: {0}
                - updatedAt: {1}

                ### 사용자 선택 필수 관광지 (꼭 방문해야 함!)
                - {2}

                ### 사용자 선택 필수 맛집 (가능하면 식사 동선에 반영!)
                - {3}

                ### 사용자 선택 필수 숙소 (숙박 동선에 반영!)
                - {4}

                ### TourAPI 장소 후보 목록 (title/lat/lng)
                {5}

                ### 채팅 로그 (DB에서 불러옴)
                {6}
                """,
                roomId,
                updatedAt,
                selectedPlaceInfo,
                selectedRestaurantInfo,
                selectedStayInfo,
                (candidates == null || candidates.isBlank()) ? "- (후보 없음)" : candidates,
                (chatLogFromDb == null || chatLogFromDb.isBlank()) ? "(채팅 없음)" : chatLogFromDb
        ));


        String result = chatModel.call(systemMessage, userMessage);

        // 생성된 여행계획 JSON 저장
        travelPlanService.save(roomId, result);

        // result 변수 안에 이미 JSON 문자열이 들어있으므로, 이를 그대로 바디에 담아 보냅니다.
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }
}


