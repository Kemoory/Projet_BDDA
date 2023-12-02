import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Record {
    private TableInfo tabInfo;
    private String[] recValues;

    public Record(TableInfo tabInfo) {
        this.tabInfo = tabInfo;
        this.recValues = new String[tabInfo.getNumberOfColumns()];
    }

    public TableInfo getTabInfo() {
        return tabInfo;
    }

    public String[] getRecValues() {
        return recValues;
    }

    public void setRecValues(String[] recValues) {
        this.recValues = recValues;
    }


    public int writeToBuffer(ByteBuffer buffer, int pos) {
        buffer.position(pos);

        for (int i = 0; i < recValues.length; i++) {
            String value = recValues[i];
            DataType colType = tabInfo.getColumns()[i].getType();
            System.out.println(pos);

            switch (colType) {
                case INT:
                    int intValue = Integer.parseInt(value);
                    buffer.putInt(intValue);
                    break;
                case FLOAT:
                    float floatValue = Float.parseFloat(value);
                    buffer.putFloat(floatValue);
                    break;
                case STRING:
                    writeFixedString(buffer, value, tabInfo.getColumns()[i].getT());
                    break;
                case VARSTRING:
                    writeVariableString(buffer, value);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported column type: " + colType);
            }
        }

        return buffer.position() - pos;
    }

    private void writeFixedString(ByteBuffer buffer, String value, int maxLength) {
        for(int i=0; i<maxLength;i++) {
            if(i<value.length()) {
                buffer.putChar(value.charAt(i));
            } else {
                buffer.putChar(' ');
            }
        }
    }

    // TODO: fix bad method
    private void writeVariableString(ByteBuffer buffer, String value) {
        byte[] stringBytes = value.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(stringBytes.length);
        buffer.put(stringBytes);
    }

    public int readFromBuffer(ByteBuffer buffer, int pos) {
        buffer.position(pos);

        for (int i = 0; i < tabInfo.getNumberOfColumns(); i++) {
            DataType colType = tabInfo.getColumns()[i].getType();

            switch (colType) {
                case INT:
                    int intValue = buffer.getInt();
                    recValues[i] = String.valueOf(intValue);
                    break;
                case FLOAT:
                    float floatValue = buffer.getFloat();
                    recValues[i] = String.valueOf(floatValue);
                    break;
                case STRING:
                    recValues[i] = readFixedString(buffer, tabInfo.getColumns()[i].getT());
                    break;
                case VARSTRING:
                    recValues[i] = readVariableString(buffer);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported column type: " + colType);
            }
        }

        return buffer.position() - pos;
    }

    private String readFixedString(ByteBuffer buffer, int maxLength) {
        StringBuffer sb = new StringBuffer();
        for(int i=0; i<maxLength; i++) {
            sb.append(buffer.getChar());
        }
        return sb.toString();
    }

    // TODO: bad method
    private String readVariableString(ByteBuffer buffer) {
        int stringLength = buffer.getInt();
        byte[] stringBytes = new byte[stringLength];
        buffer.get(stringBytes);
        return new String(stringBytes, StandardCharsets.UTF_8);
    }

    public int getSize() {
        int size = 0;

        for (int i = 0; i < recValues.length; i++) {
            String value = recValues[i];
            DataType colType = tabInfo.getColumns()[i].getType();

            switch (colType) {
                case INT:
                    size += Integer.BYTES;
                    break;
                case FLOAT:
                    size += Float.BYTES;
                    break;
                case STRING:
                    size += Character.BYTES * tabInfo.getColumns()[i].getT();
                    break;
                case VARSTRING:
                    size += Character.BYTES * value.length();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported column type: " + colType);
            }
        }

        return size;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Record: (");
        for (String val : recValues) {
            sb.append(val).append(", ");
        }
        sb.append(")");
        return sb.toString();
    }

    // New method to get all records
    public static List<Record> getAllRecords(TableInfo tabInfo) throws IOException {
        FileManager fileManager = FileManager.getInstance();
        List<PageId> dataPages = fileManager.getDataPages(tabInfo);
        List<Record> records = new ArrayList<>();

        for (PageId pageId : dataPages) {
            records.addAll(fileManager.getRecordsInDataPage(tabInfo, pageId));
        }

        return records;
    }
}
