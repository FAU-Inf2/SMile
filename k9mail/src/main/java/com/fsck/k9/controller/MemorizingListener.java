package com.fsck.k9.controller;

import com.fsck.k9.Account;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MemorizingListener extends MessagingListener {
    Map<String, Memory> memories = new HashMap<String, Memory>(31);

    Memory getMemory(Account account, String folderName) {
        Memory memory = memories.get(MessagingController.getMemoryKey(account, folderName));
        if (memory == null) {
            memory = new Memory(account, folderName);
            memories.put(memory.getKey(), memory);
        }
        return memory;
    }

    synchronized void removeAccount(Account account) {
        Iterator<Map.Entry<String, Memory>> memIt = memories.entrySet().iterator();

        while (memIt.hasNext()) {
            Map.Entry<String, Memory> memoryEntry = memIt.next();

            String uuidForMemory = memoryEntry.getValue().getAccount().getUuid();

            if (uuidForMemory.equals(account.getUuid())) {
                memIt.remove();
            }
        }
    }

    @Override
    public synchronized void synchronizeMailboxStarted(Account account, String folder) {
        Memory memory = getMemory(account, folder);
        memory.setSyncingState(Memory.MemorizingState.STARTED);
        memory.setFolderCompleted(0);
        memory.setFolderTotal(0);
    }

    @Override
    public synchronized void synchronizeMailboxFinished(Account account, String folder,
                                                        int totalMessagesInMailbox, int numNewMessages) {
        Memory memory = getMemory(account, folder);
        memory.setSyncingState(Memory.MemorizingState.FINISHED);
        memory.setSyncingTotalMessagesInMailbox(totalMessagesInMailbox);
        memory.setSyncingNumNewMessages(numNewMessages);
    }

    @Override
    public synchronized void synchronizeMailboxFailed(Account account, String folder,
                                                      String message) {

        Memory memory = getMemory(account, folder);
        memory.setSyncingState(Memory.MemorizingState.FAILED);
        memory.setFailureMessage(message);
    }

    synchronized void refreshOther(MessagingListener other) {
        if (other != null) {

            Memory syncStarted = null;
            Memory sendStarted = null;
            Memory processingStarted = null;

            for (Memory memory : memories.values()) {

                if (memory.getSyncingState() != null) {
                    switch (memory.getSyncingState()) {
                        case STARTED:
                            syncStarted = memory;
                            break;
                        case FINISHED:
                            other.synchronizeMailboxFinished(memory.getAccount(), memory.getFolderName(),
                                    memory.getSyncingTotalMessagesInMailbox(), memory.getSyncingNumNewMessages());
                            break;
                        case FAILED:
                            other.synchronizeMailboxFailed(memory.getAccount(), memory.getFolderName(),
                                    memory.getFailureMessage());
                            break;
                    }
                }

                if (memory.getSendingState() != null) {
                    switch (memory.getSendingState()) {
                        case STARTED:
                            sendStarted = memory;
                            break;
                        case FINISHED:
                            other.sendPendingMessagesCompleted(memory.getAccount());
                            break;
                        case FAILED:
                            other.sendPendingMessagesFailed(memory.getAccount());
                            break;
                    }
                }
                if (memory.getPushingState() != null) {
                    switch (memory.getPushingState()) {
                        case STARTED:
                            other.setPushActive(memory.getAccount(), memory.getFolderName(), true);
                            break;
                        case FINISHED:
                            other.setPushActive(memory.getAccount(), memory.getFolderName(), false);
                            break;
                        case FAILED:
                            break;
                    }
                }
                if (memory.getProcessingState() != null) {
                    switch (memory.getProcessingState()) {
                        case STARTED:
                            processingStarted = memory;
                            break;
                        case FINISHED:
                        case FAILED:
                            other.pendingCommandsFinished(memory.getAccount());
                            break;
                    }
                }
            }
            Memory somethingStarted = null;
            if (syncStarted != null) {
                other.synchronizeMailboxStarted(syncStarted.getAccount(), syncStarted.getFolderName());
                somethingStarted = syncStarted;
            }
            if (sendStarted != null) {
                other.sendPendingMessagesStarted(sendStarted.getAccount());
                somethingStarted = sendStarted;
            }
            if (processingStarted != null) {
                other.pendingCommandsProcessing(processingStarted.getAccount());
                if (processingStarted.getProcessingCommandTitle() != null) {
                    other.pendingCommandStarted(processingStarted.getAccount(), processingStarted.getProcessingCommandTitle());

                } else {
                    other.pendingCommandCompleted(processingStarted.getAccount(), processingStarted.getProcessingCommandTitle());
                }
                somethingStarted = processingStarted;
            }
            if (somethingStarted != null && somethingStarted.getFolderTotal() > 0) {
                other.synchronizeMailboxProgress(somethingStarted.getAccount(), somethingStarted.getFolderName(), somethingStarted.getFolderCompleted(), somethingStarted.getFolderTotal());
            }

        }
    }

    @Override
    public synchronized void setPushActive(Account account, String folderName, boolean active) {
        Memory memory = getMemory(account, folderName);
        memory.setPushingState((active ? Memory.MemorizingState.STARTED : Memory.MemorizingState.FINISHED));
    }

    @Override
    public synchronized void sendPendingMessagesStarted(Account account) {
        Memory memory = getMemory(account, null);
        memory.setSendingState(Memory.MemorizingState.STARTED);
        memory.setFolderCompleted(0);
        memory.setFolderTotal(0);
    }

    @Override
    public synchronized void sendPendingMessagesCompleted(Account account) {
        Memory memory = getMemory(account, null);
        memory.setSendingState(Memory.MemorizingState.FINISHED);
    }

    @Override
    public synchronized void sendPendingMessagesFailed(Account account) {
        Memory memory = getMemory(account, null);
        memory.setSendingState(Memory.MemorizingState.FAILED);
    }


    @Override
    public synchronized void synchronizeMailboxProgress(Account account, String folderName, int completed, int total) {
        Memory memory = getMemory(account, folderName);
        memory.setFolderCompleted(completed);
        memory.setFolderTotal(total);
    }


    @Override
    public synchronized void pendingCommandsProcessing(Account account) {
        Memory memory = getMemory(account, null);
        memory.setProcessingState(Memory.MemorizingState.STARTED);
        memory.setFolderCompleted(0);
        memory.setFolderTotal(0);
    }

    @Override
    public synchronized void pendingCommandsFinished(Account account) {
        Memory memory = getMemory(account, null);
        memory.setProcessingState(Memory.MemorizingState.FINISHED);
    }

    @Override
    public synchronized void pendingCommandStarted(Account account, String commandTitle) {
        Memory memory = getMemory(account, null);
        memory.setProcessingCommandTitle(commandTitle);
    }

    @Override
    public synchronized void pendingCommandCompleted(Account account, String commandTitle) {
        Memory memory = getMemory(account, null);
        memory.setProcessingCommandTitle(null);
    }

}
