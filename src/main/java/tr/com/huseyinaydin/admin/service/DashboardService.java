package tr.com.huseyinaydin.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.huseyinaydin.auth.repository.AppUserRepository;
import tr.com.huseyinaydin.message.repository.MailMessageRepository;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final AppUserRepository userRepository;
    private final MailMessageRepository messageRepository;

    @Transactional(readOnly = true)
    public DashboardData getDashboard() {
        var today = LocalDate.now(ZoneOffset.UTC);
        var startOfToday = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        var startOfTomorrow = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return new DashboardData(
                userRepository.count(),
                userRepository.countByActiveTrue(),
                messageRepository.countByDraftFalse(),
                messageRepository.countByDraftFalseAndSentAtGreaterThanEqualAndSentAtLessThan(startOfToday, startOfTomorrow),
                messageRepository.countByDraftFalseAndTrashFalseAndReadFalse(),
                messageRepository.countByTrashTrue(),
                messageRepository.findTopSenders(PageRequest.of(0, 5)).stream()
                        .map(row -> new RankingItem(row[0] + " " + row[1], ((Number) row[2]).longValue())).toList(),
                messageRepository.findTopCategories(PageRequest.of(0, 5)).stream()
                        .map(row -> new RankingItem((String) row[0], ((Number) row[1]).longValue())).toList()
        );
    }
}
