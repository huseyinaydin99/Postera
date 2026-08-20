package tr.com.huseyinaydin.admin.service;

import java.util.List;

public record DashboardData(long totalUsers, long activeUsers, long totalMessages, long todayMessages,
                            long unreadMessages, long trashMessages, List<RankingItem> topSenders,
                            List<RankingItem> topCategories) {
}
