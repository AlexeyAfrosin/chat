package client;

public interface HistoryService {
    void writeHistory(String message);
    String getHistory(int countLastMessage);
    void closeConnection();
}
