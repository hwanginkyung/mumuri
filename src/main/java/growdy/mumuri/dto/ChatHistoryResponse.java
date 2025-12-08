package growdy.mumuri.dto;

import java.util.List;

public record ChatHistoryResponse(
        List<ChatMessageResponse> messages,
        boolean hasNext,
        Long nextCursor   // 다음 요청 때 ?cursor= 로 넘길 값 (null이면 더 없음)
) {
}