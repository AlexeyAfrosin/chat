package client;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class FileHistoryService implements HistoryService {

    private BufferedWriter fileWriter;
    private String fileName;

    public FileHistoryService(String login) {
        try {
            this.fileName = getFileName(login);
            this.fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.fileName, true), StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeHistory(String message) {
        try {
            fileWriter.write(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getHistory(int countLastMessage) {
        if (countLastMessage > 0) {

            RandomAccessFile randomAccessFile = null;
            try {
                randomAccessFile = new RandomAccessFile(fileName, "r");
                long fileLength = randomAccessFile.length() - 1;

                int tmpReadLength = Constants.READ_HISTORY_PATH_LENGTH;

                String[] historyStr;
                long startPosition;
                if (fileLength > 0) {
                    if (tmpReadLength > fileLength) {
                        tmpReadLength = (int) fileLength;
                    }

                    do {
                        byte[] buf = new byte[tmpReadLength];
                        startPosition = fileLength - tmpReadLength;
                        randomAccessFile.seek(startPosition);

                        randomAccessFile.read(buf);

                        historyStr = new String(buf, StandardCharsets.UTF_8).split(Constants.MESSAGE_DELIMITER);

                        if ((historyStr.length >= Constants.HISTORY_LAST_MESSAGE_COUNT) || (fileLength - tmpReadLength <= 0)) {
                            int startIdx = 0;
                            String endString = "\n\n";
                            int endIdx = historyStr.length;
                            if (historyStr.length - Constants.HISTORY_LAST_MESSAGE_COUNT > 0) {
                                startIdx = historyStr.length - Constants.HISTORY_LAST_MESSAGE_COUNT;
                            }

                            return (String.join("\n", Arrays.copyOfRange(historyStr, startIdx, endIdx))).trim() + endString;
                        }

                        if (tmpReadLength + Constants.READ_HISTORY_PATH_LENGTH > fileLength) {
                            tmpReadLength = (int) fileLength;
                        } else {
                            tmpReadLength = tmpReadLength + Constants.READ_HISTORY_PATH_LENGTH;
                        }

                    } while (true);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (randomAccessFile != null) {
                    try {
                        randomAccessFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return "";
    }

    @Override
    public void closeConnection() {
        try {
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFileName(String login) {
        File dir = new File(Constants.HISTORY_FILE_PATH);
        if (!dir.exists()) {
            dir.mkdir();
        }
        return dir.getPath() + "/history_" + login + ".txt";
    }
}
