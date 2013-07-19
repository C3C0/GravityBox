package com.ceco.gm2.gravitybox;

import static de.robv.android.xposed.XposedHelpers.findField;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import libcore.io.BufferIterator;
import libcore.io.HeapBufferIterator;
import libcore.io.Streams;

public class PatchMasterKey {
    private static final String TAG = "PatchMasterKey";

    private static final long CENSIG = 0x2014b50;
    private static final int GPBF_ENCRYPTED_FLAG = 1 << 0;
    private static final int GPBF_UNSUPPORTED_MASK = GPBF_ENCRYPTED_FLAG;
    private static final int ENDHDR = 22;
    private static final int CENHDR = 46;

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static Field fldCompressionMethod;
    private static Field fldTime;
    private static Field fldModDate;
    private static Field fldNameLength;
    private static Field fldLocalHeaderRelOffset;
    private static Field fldName;
    private static Field fldRaf;
    private static Field fldEntries;

    static {
        fldCompressionMethod = findField(ZipEntry.class, "compressionMethod");
        fldTime = findField(ZipEntry.class, "time");
        fldModDate = findField(ZipEntry.class, "modDate");
        fldNameLength = findField(ZipEntry.class, "nameLength");
        try {
            fldLocalHeaderRelOffset = findField(ZipEntry.class, "localHeaderRelOffset");
        } catch (Throwable t) {
            fldLocalHeaderRelOffset = findField(ZipEntry.class, "mLocalHeaderRelOffset");
        }
        fldName = findField(ZipEntry.class, "name");
        try {
            fldRaf = findField(ZipFile.class, "mRaf");
        } catch (Throwable t) {
            fldRaf = findField(ZipFile.class, "raf");
        }
        try {
            fldEntries = findField(ZipFile.class, "mEntries");
        } catch (Throwable t) {
            fldEntries = findField(ZipFile.class, "entries");
        }
    }

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void initZygote() {
        try {
            log("Patching MasterKey vulnerabilities");

            XposedHelpers.findAndHookMethod(ZipFile.class, "readCentralDir", new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        readCentralDir((ZipFile) param.thisObject);
                        param.setResult(null);
                    } catch (Exception ex) {
                        param.setThrowable(ex);
                    }
                }
            });
        } catch(Exception e) {
            XposedBridge.log(e);
        }
    }

    private static void readCentralDir(ZipFile zipFile) throws IOException {
        try {
            /*
             * Scan back, looking for the End Of Central Directory field. If
             * the archive doesn't have a comment, we'll hit it on the first
             * try.
             * 
             * No need to synchronize mRaf here -- we only do this when we
             * first open the Zip file.
             */

            RandomAccessFile mRaf = (RandomAccessFile) fldRaf.get(zipFile);
            long scanOffset = mRaf.length() - ENDHDR;
            if (scanOffset < 0) {
                throw new ZipException("File too short to be a zip file: " + mRaf.length());
            }
    
            long stopOffset = scanOffset - 65536;
            if (stopOffset < 0) {
                stopOffset = 0;
            }
    
            final int ENDHEADERMAGIC = 0x06054b50;
            while (true) {
                mRaf.seek(scanOffset);
                if (Integer.reverseBytes(mRaf.readInt()) == ENDHEADERMAGIC) {
                    break;
                }
    
                scanOffset--;
                if (scanOffset < stopOffset) {
                    throw new ZipException("EOCD not found; not a Zip archive?");
                }
            }
    
            // Read the End Of Central Directory. We could use ENDHDR instead of the magic number 18,
            // but we don't actually need all the header.
            byte[] eocd = new byte[18];
            mRaf.readFully(eocd);
    
            // Pull out the information we need.
            BufferIterator it = HeapBufferIterator.iterator(eocd, 0, eocd.length, ByteOrder.LITTLE_ENDIAN);
            int diskNumber = it.readShort() & 0xffff;
            int diskWithCentralDir = it.readShort() & 0xffff;
            int numEntries = it.readShort() & 0xffff;
            int totalNumEntries = it.readShort() & 0xffff;
            it.skip(4); // Ignore centralDirSize.
            int centralDirOffset = it.readInt();
    
            if (numEntries != totalNumEntries || diskNumber != 0 || diskWithCentralDir != 0) {
                throw new ZipException("spanned archives not supported");
            }
    
            // Seek to the first CDE and read all entries.
            RAFStream rafs = new RAFStream(mRaf, centralDirOffset);
            BufferedInputStream bin = new BufferedInputStream(rafs, 4096);
            byte[] hdrBuf = new byte[CENHDR]; // Reuse the same buffer for each entry.
            @SuppressWarnings("unchecked")
            Map<String, ZipEntry> mEntries = (Map<String, ZipEntry>) fldEntries.get(zipFile);
            for (int i = 0; i < numEntries; ++i) {
                ZipEntry newEntry = loadFromStream(hdrBuf, bin);
                String entryName = newEntry.getName();
                if (mEntries.put(entryName, newEntry) != null) {
                    throw new ZipException("Duplicate entry name: " + entryName);
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    private static class RAFStream extends InputStream {

        RandomAccessFile mSharedRaf;
        long mOffset;
        long mLength;

        public RAFStream(RandomAccessFile raf, long pos) throws IOException {
            mSharedRaf = raf;
            mOffset = pos;
            mLength = raf.length();
        }

        @Override
        public int available() throws IOException {
            return (mOffset < mLength ? 1 : 0);
        }

        @Override
        public int read() throws IOException {
            return Streams.readSingleByte(this);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            synchronized (mSharedRaf) {
                mSharedRaf.seek(mOffset);
                if (len > mLength - mOffset) {
                    len = (int) (mLength - mOffset);
                }
                int count = mSharedRaf.read(b, off, len);
                if (count > 0) {
                    mOffset += count;
                    return count;
                } else {
                    return -1;
                }
            }
        }

        @Override
        public long skip(long byteCount) throws IOException {
            if (byteCount > mLength - mOffset) {
                byteCount = mLength - mOffset;
            }
            mOffset += byteCount;
            return byteCount;
        }
    }

    private static ZipEntry loadFromStream(byte[] hdrBuf, BufferedInputStream in) throws IOException {
        try {
            ZipEntry result = new ZipEntry("");

            Streams.readFully(in, hdrBuf, 0, hdrBuf.length);

            BufferIterator it = HeapBufferIterator.iterator(hdrBuf, 0, hdrBuf.length, ByteOrder.LITTLE_ENDIAN);

            int sig = it.readInt();
            if (sig != CENSIG) {
                throw new ZipException("Central Directory Entry not found");
            }

            it.seek(8);
            int gpbf = it.readShort() & 0xffff;

            if ((gpbf & GPBF_UNSUPPORTED_MASK) != 0) {
                throw new ZipException("Invalid General Purpose Bit Flag: " + gpbf);
            }

            int compressionMethod;
            compressionMethod = it.readShort() & 0xffff;
            fldCompressionMethod.setInt(result, compressionMethod);
            int time;
            time = it.readShort() & 0xffff;
            fldTime.setInt(result, time);
            int modDate;
            modDate = it.readShort() & 0xffff;
            fldModDate.setInt(result, modDate);

            // These are 32-bit values in the file, but 64-bit fields in this object.
            long crc;
            crc = ((long) it.readInt()) & 0xffffffffL;
            result.setCrc(crc);
            long compressedSize;
            compressedSize = ((long) it.readInt()) & 0xffffffffL;
            result.setCompressedSize(compressedSize);
            long size;
            size = ((long) it.readInt()) & 0xffffffffL;
            result.setSize(size);

            int nameLength;
            nameLength = it.readShort() & 0xffff;
            fldNameLength.setInt(result, nameLength);
            int extraLength = it.readShort() & 0xffff;
            int commentByteCount = it.readShort() & 0xffff;

            // This is a 32-bit value in the file, but a 64-bit field in this object.
            it.seek(42);
            long localHeaderRelOffset;
            localHeaderRelOffset = ((long) it.readInt()) & 0xffffffffL;
            fldLocalHeaderRelOffset.setLong(result, localHeaderRelOffset);

            byte[] nameBytes = new byte[nameLength];
            Streams.readFully(in, nameBytes, 0, nameBytes.length);
            String name;
            name = new String(nameBytes, 0, nameBytes.length, UTF_8);
            fldName.set(result, name);

            // The RI has always assumed UTF-8. (If GPBF_UTF8_FLAG isn't set, the encoding is
            // actually IBM-437.)
            if (commentByteCount > 0) {
                byte[] commentBytes = new byte[commentByteCount];
                Streams.readFully(in, commentBytes, 0, commentByteCount);
                String comment;
                comment = new String(commentBytes, 0, commentBytes.length, UTF_8);
                result.setComment(comment);

            }

            if (extraLength > 0) {
                byte[] extra;
                extra = new byte[extraLength];
                Streams.readFully(in, extra, 0, extraLength);
                result.setExtra(extra);
            }
            return result;
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }
}