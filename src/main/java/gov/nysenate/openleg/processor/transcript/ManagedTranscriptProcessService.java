package gov.nysenate.openleg.processor.transcript;

import gov.nysenate.openleg.dao.base.LimitOffset;
import gov.nysenate.openleg.dao.transcript.TranscriptFileDao;
import gov.nysenate.openleg.model.transcript.TranscriptFile;
import gov.nysenate.openleg.model.transcript.TranscriptId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ManagedTranscriptProcessService implements TranscriptProcessService
{
    private static Logger logger = LoggerFactory.getLogger(ManagedTranscriptProcessService.class);

    @Autowired
    private TranscriptFileDao transcriptFileDao;

    @Autowired
    private TranscriptParser transcriptParser;


    /** --- Implemented Methods --- */

    /** {@inheritDoc} */
    @Override
    public int collateTranscriptFiles() {
        logger.info("Collating transcript files...");
        int numCollated = 0;
        try {
            List<TranscriptFile> transcriptFiles;
            do {
                transcriptFiles = transcriptFileDao.getIncomingTranscriptFiles(LimitOffset.FIFTY);
                for (TranscriptFile file : transcriptFiles) {
                    file.setPendingProcessing(true);
                    transcriptFileDao.archiveAndUpdateTranscriptFile(file);
                    numCollated++;
                }
            }
            while (!transcriptFiles.isEmpty());
        }
        catch (IOException ex) {
            logger.error("Error retrieving transcript files during collation", ex);
        }
        logger.info("Collated {} transcript files.", numCollated);
        return numCollated;
    }

    /** {@inheritDoc} */
    @Override
    public List<TranscriptFile> getPendingTranscriptFiles(LimitOffset limitOffset) {
        return transcriptFileDao.getPendingTranscriptFiles(limitOffset);
    }

    /** {@inheritDoc} */
    @Override
    public void processTranscriptFiles(List<TranscriptFile> transcriptFiles) {
        for (TranscriptFile file : transcriptFiles) {
            try {
                logger.info("Processing transcript file {}", file.getFileName());
                transcriptParser.process(file);
                file.setProcessedCount(file.getProcessedCount() + 1);
                file.setPendingProcessing(false);
                file.setProcessedDateTime(LocalDateTime.now());
                transcriptFileDao.updateTranscriptFile(file);
            }
            catch (IOException ex) {
                logger.error("Error processing TranscriptFile " + file.getFileName() + ".", ex);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void processPendingTranscriptFiles() {
        List<TranscriptFile> transcriptFiles;
        do {
            transcriptFiles = getPendingTranscriptFiles(LimitOffset.FIFTY);
            processTranscriptFiles(transcriptFiles);
        }
        while (!transcriptFiles.isEmpty());
    }

    /** {@inheritDoc} */
    @Override
    public void updatePendingProcessing(TranscriptId transcriptId, boolean pendingProcessing) {
        throw new UnsupportedOperationException("Not implemented");
    }
}