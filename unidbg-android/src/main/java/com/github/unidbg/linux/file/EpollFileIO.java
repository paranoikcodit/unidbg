package com.github.unidbg.linux.file;

import com.github.unidbg.Emulator;
import com.github.unidbg.file.linux.BaseAndroidFileIO;

import java.util.HashMap;
import java.util.Map;

public class EpollFileIO extends BaseAndroidFileIO {

    private final Map<Integer, Integer> watchMap = new HashMap<>();
    private static final int O_NONBLOCK = 04000;
    private boolean nonBlocking;

    public EpollFileIO(int oflags) {
        super(oflags);
    }

    @Override
    public int write(byte[] data) {
        return -1;
    }

    public int waitEvents(Emulator<?> emu, long eventsPtr, int maxEvents, int timeout) {
        if (nonBlocking && timeout != 0) {
            return -11;  // -EAGAIN
        }

        return 0;
    }

    @Override
    public void close() {
        watchMap.clear();
    }

    @Override
    protected void setFlags(long arg) {
        int newFlags = (int) arg;

        if ((newFlags & O_NONBLOCK) != 0) {
            nonBlocking = true;
            this.oflags |= O_NONBLOCK;
        } else {
            nonBlocking = false;
            this.oflags &= ~O_NONBLOCK;
        }
    }

    @Override
    public int fcntl(Emulator<?> emulator, int cmd, long arg) {
        switch (cmd) {
            case 3:   /* F_GETFL */
                return oflags;
            case 4:   /* F_SETFL */
                setFlags(arg);
                return 0;
            default:
                return -1;          // EINVAL
        }
    }

    public int ctl(int fd, int op, int events) {
        switch (op) {
            case 1: /* EPOLL_CTL_ADD */
                watchMap.put(fd, events);
                break;
            case 2: /* EPOLL_CTL_DEL */
                watchMap.remove(fd);
                break;
            case 3: /* EPOLL_CTL_MOD */
                watchMap.put(fd, events);
                break;
            default:
                return -22; // -EINVAL
        }
        return 0;
    }
}
