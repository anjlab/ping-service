package dmitrygusev.ping.services;

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class DataNucleusTransactionLoggingFilter implements Filter {

    @Override
    public boolean isLoggable(LogRecord record)
    {
        if (!Utils.isNullOrEmpty(record.getMessage())
            && record.getMessage().startsWith("Operation commit failed on resource"))
        {
            record.setLevel(Level.WARNING);
        }
        return true;
    }

}
