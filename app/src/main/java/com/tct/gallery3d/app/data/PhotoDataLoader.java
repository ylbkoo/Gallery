/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*------------|-----------------|-------------------|------------------------------------------------------------------------------*/
/* 03/07/2015 |    su.jiang     |      PR-1034387   |[SW][Gallery][FC]Gallery will fc when check picture's details after delete it */
/*------------|-----------------|-------------------|------------------------------------------------------------------------------*/
/* 06/07/2015|dongliang.feng        |PR999019              |[5.0][Gallery] wrong toast when entering Video category */
/* ----------|----------------------|----------------------|----------------- */
/* 07/15/2015| jian.pan1            | PR1006938            |[5.0][Gallery] camera roll picture repeatative refreshing
/* ----------|----------------------|----------------------|----------------- */

package com.tct.gallery3d.app.data;

import android.os.Handler;
import android.os.Message;
import android.os.Process;

import com.tct.gallery3d.app.AbstractGalleryActivity;
import com.tct.gallery3d.app.LoadingListener;
import com.tct.gallery3d.app.Log;
import com.tct.gallery3d.common.Utils;
import com.tct.gallery3d.data.ContentListener;
import com.tct.gallery3d.data.LocalImage;
import com.tct.gallery3d.data.MediaItem;
import com.tct.gallery3d.data.MediaObject;
import com.tct.gallery3d.data.MediaSet;
import com.tct.gallery3d.data.Path;
import com.tct.gallery3d.picturegrouping.ExifInfoFilter;
import com.tct.gallery3d.util.GalleryUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class PhotoDataLoader {
    private static final String TAG = "PhotoDetailDataLoader";

    private final AbstractGalleryActivity mActivity;
    private final MediaSet mSource;
    private DataListener mDataListener;
    private MySourceListener mSourceListener = new MySourceListener();
    private LoadingListener mLoadingListener;
    private ReloadTask mReloadTask;

    private Handler mMainHandler;

    private long mVersion = 0;
    private long mSourceVersion = MediaObject.INVALID_DATA_VERSION;
    private long mFailedVersion = MediaObject.INVALID_DATA_VERSION;

    private static final int MIN_LOAD_COUNT = 5;
    private static final int MAX_LOAD_COUNT = 10;
    private static final int DATA_CACHE_SIZE = 20;

    private static final int MSG_LOAD_START = 1;
    private static final int MSG_LOAD_FINISH = 2;
    private static final int MSG_RUN_OBJECT = 3;

    private int mMediaSetType = MediaSet.MEDIASET_TYPE_UNKNOWN;
    private int mSize = 0;
    private final MediaItem[] mData;
    private final long[] mSetVersion;
    private final long[] mItemVersion;

    public interface DataListener {
        void onContentChanged(int index);
        void onSizeChanged(int size);
    }

    private int mActiveStart = 0;
    private int mActiveEnd = 0;
    private int mContentStart = 0;
    private int mContentEnd = 0;

    public PhotoDataLoader(AbstractGalleryActivity context, MediaSet mediaSet) {
        mActivity = context;
        mSource = mediaSet;

        mData = new MediaItem[DATA_CACHE_SIZE];
        mSetVersion = new long[DATA_CACHE_SIZE];
        mItemVersion = new long[DATA_CACHE_SIZE];

        Arrays.fill(mItemVersion, MediaObject.INVALID_DATA_VERSION);
        Arrays.fill(mSetVersion, MediaObject.INVALID_DATA_VERSION);

        mMainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case MSG_RUN_OBJECT:
                    ((Runnable) msg.obj).run();
                    break;
                case MSG_LOAD_START:
                    if (mLoadingListener != null) {
                        mLoadingListener.onLoadingStarted();
                    }
                    break;
                case MSG_LOAD_FINISH:
                    if (mLoadingListener != null) {
                        mLoadingListener.onLoadingFinished(false);
                    }
                    return;
                }
            }
        };
    }

    public void resume() {
        if (mSource != null) {
            mSource.addContentListener(mSourceListener);
        }
        mReloadTask = new ReloadTask();
        mReloadTask.start();
    }

    public void pause() {
        /* MODIFIED-BEGIN by Yaoyu.Yang, 2016-08-19,BUG-2208330*/
        if (mReloadTask != null) {
            mReloadTask.terminate();
            mReloadTask = null;
        }
        /* MODIFIED-END by Yaoyu.Yang,BUG-2208330*/
        mSource.removeContentListener(mSourceListener);
    }

    public int getMediaSetType() {
        return mMediaSetType;
    }

    public int getMediaItemCount() {
        return mSize;
    }

    public int size() {
        return mSize;
    }

    public MediaItem get(int index) {
        if (!isActive(index)) {
//            return mSource.getMediaItem(index, 1).get(0);
        }
        return mData[index % mData.length];
    }

    public int getActiveStart() {
        return mActiveStart;
    }

    public int getActiveEnd() {
        return mActiveEnd;
    }

    public boolean isActive(int index) {
        return index >= mActiveStart && index < mActiveEnd;
    }

    // Returns the index of the MediaItem with the given path or
    // -1 if the path is not cached
    public int findItem(Path id) {
        for (int i = mContentStart; i < mContentEnd; i++) {
            MediaItem item = mData[i % DATA_CACHE_SIZE];
            if (item != null && id == item.getPath()) {
                return i;
            }
        }
        return -1;
    }

    private void clearSlot(int slotIndex) {
        mData[slotIndex] = null;
        mItemVersion[slotIndex] = MediaObject.INVALID_DATA_VERSION;
        mSetVersion[slotIndex] = MediaObject.INVALID_DATA_VERSION;
    }

    private void setContentWindow(int contentStart, int contentEnd) {
        if (contentStart == mContentStart && contentEnd == mContentEnd)
            return;
        int end = mContentEnd;
        int start = mContentStart;

        // We need change the content window before calling reloadData(...)
        synchronized (this) {
            mContentStart = contentStart;
            mContentEnd = contentEnd;
        }
        if (contentStart >= end || start >= contentEnd) {
            for (int i = start, n = end; i < n; ++i) {
                clearSlot(i % DATA_CACHE_SIZE);
            }
        } else {
            for (int i = start; i < contentStart; ++i) {
                clearSlot(i % DATA_CACHE_SIZE);
            }
            for (int i = contentEnd, n = end; i < n; ++i) {
                clearSlot(i % DATA_CACHE_SIZE);
            }
        }
        if (mReloadTask != null)
            mReloadTask.notifyDirty();
    }

    public void setActiveWindow(int start, int end) {
        if (start == mActiveStart && end == mActiveEnd)
            return;

        Utils.assertTrue(start <= end && end - start <= mData.length && end <= mSize);

        int length = mData.length;
        mActiveStart = start;
        mActiveEnd = end;

        // If no data is visible, keep the cache content
        if (start == end)
            return;

        int contentStart = Utils.clamp((start + end) / 2 - length / 2, 0, Math.max(0, mSize - length));
        int contentEnd = Math.min(contentStart + length, mSize);
        if (mContentStart > start || mContentEnd < end || Math.abs(contentStart - mContentStart) > MIN_LOAD_COUNT) {
            setContentWindow(contentStart, contentEnd);
        }
    }

    private class MySourceListener implements ContentListener {
        @Override
        public void onContentDirty() {
            if (mReloadTask != null)
                mReloadTask.notifyDirty();
        }
    }

    public void setDataListener(DataListener listener) {
        mDataListener = listener;
    }

    public void setLoadingListener(LoadingListener listener) {
        mLoadingListener = listener;
    }

    private <T> T executeAndWait(Callable<T> callable) {
        FutureTask<T> task = new FutureTask<T>(callable);
        mMainHandler.removeMessages(MSG_RUN_OBJECT);
        mMainHandler.sendMessageAtFrontOfQueue(mMainHandler.obtainMessage(MSG_RUN_OBJECT, task));
        try {
            return task.get();
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static class UpdateInfo {
        public long version;
        public int reloadStart;
        public int reloadCount;

        public int size;
        public ArrayList<MediaItem> items;
    }

    private class GetUpdateInfo implements Callable<UpdateInfo> {
        private final long mVersion;

        public GetUpdateInfo(long version) {
            mVersion = version;
        }

        @Override
        public UpdateInfo call() throws Exception {
            if (mFailedVersion == mVersion) {
                // previous loading failed, return null to pause loading
                return null;
            }
            UpdateInfo info = new UpdateInfo();
            long version = mVersion;
            info.version = mSourceVersion;
            info.size = mSize;
            long setVersion[] = mSetVersion;
            for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
                int index = i % DATA_CACHE_SIZE;
                if (setVersion[index] != version) {
                    info.reloadStart = i;
                    info.reloadCount = Math.min(MAX_LOAD_COUNT, n - i);
                    return info;
                }
            }
            return mSourceVersion == mVersion ? null : info;
        }
    }

    private class UpdateContent implements Callable<Void> {

        private UpdateInfo mUpdateInfo;

        public UpdateContent(UpdateInfo info) {
            mUpdateInfo = info;
        }

        @Override
        public Void call() throws Exception {
            UpdateInfo info = mUpdateInfo;
            mSourceVersion = info.version;
            if (mSize != info.size) {
                mSize = info.size;
                if (mDataListener != null)
                    mDataListener.onSizeChanged(mSize);
                if (mContentEnd > mSize)
                    mContentEnd = mSize;
                if (mActiveEnd > mSize)
                    mActiveEnd = mSize;
            }

            ArrayList<MediaItem> items = info.items;
            mFailedVersion = MediaObject.INVALID_DATA_VERSION;
            if ((items == null) || items.isEmpty()) {
                if (info.reloadCount > 0) {
                    mFailedVersion = info.version;
                    Log.d(TAG, "loading failed: " + mFailedVersion);
                }
                return null;
            }
            int start = Math.max(info.reloadStart, mContentStart);
            int end = Math.min(info.reloadStart + items.size(), mContentEnd);
            for (int i = start; i < end; ++i) {
                int index = i % DATA_CACHE_SIZE;
                mSetVersion[index] = info.version;
                MediaItem updateItem = items.get(i - info.reloadStart);
                // [BUGFIX]-Modified by TCTNJ,qiang.ding1, 2014-12-17,PR871604
                // begain
                if (updateItem != null) {
                    long itemVersion = updateItem.getDataVersion();
                    // [BUGFIX]-Modified by TCTNJ,qiang.ding1,
                    // 2014-11-28,PR849279 begain
                    // [BUGFIX]-Add by TCTNJ,jian.pan1, 2015-07-15,PR1006938
                    // begin
                    if (mItemVersion[index] != itemVersion) {
                        // [BUGFIX]-Add by TCTNJ,jian.pan1, 2015-07-15,PR1006938
                        // end
                        // [BUGFIX]-Modified by TCTNJ,qiang.ding1,
                        // 2014-11-28,PR849279 end
                        mItemVersion[index] = itemVersion;
                        mData[index] = updateItem;
                        if (mDataListener != null && i >= mActiveStart && i < mActiveEnd) {
                            mDataListener.onContentChanged(i);
                        }
                    }
                }
                //[BUGFIX]-Modified by TCTNJ,qiang.ding1, 2014-12-17,PR871604 end
            }
            return null;
        }
    }

    /*
     * The thread model of ReloadTask
     *      *
     * [Reload Task]       [Main Thread]
     *       |                   |
     * getUpdateInfo() -->       |           (synchronous call)
     *     (wait) <----    getUpdateInfo()
     *       |                   |
     *   Load Data               |
     *       |                   |
     * updateContent() -->       |           (synchronous call)
     *     (wait)          updateContent()
     *       |                   |
     *       |                   |
     */
    private class ReloadTask extends Thread {

        private volatile boolean mActive = true;
        private volatile boolean mDirty = true;
        private boolean mIsLoading = false;

        private void updateLoading(boolean loading) {
            if (mIsLoading == loading)
                return;
            mIsLoading = loading;
            mMainHandler.sendEmptyMessage(loading ? MSG_LOAD_START : MSG_LOAD_FINISH);
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            boolean updateComplete = false;
            while (mActive) {
                synchronized (this) {
                    if (mActive && !mDirty && updateComplete) {
                        if (!mSource.isLoading())
                            updateLoading(false);
                        if (mFailedVersion != MediaObject.INVALID_DATA_VERSION) {
                            Log.d(TAG, "reload pause");
                        }
                        Utils.waitWithoutInterrupt(this);
                        if (mActive && (mFailedVersion != MediaObject.INVALID_DATA_VERSION)) {
                            Log.d(TAG, "reload resume");
                        }
                        continue;
                    }
                    mDirty = false;
                }
                updateLoading(true);
                long version = mSource.reload();
                UpdateInfo info = executeAndWait(new GetUpdateInfo(version));
                updateComplete = info == null;
                if (updateComplete)
                    continue;
                if (info.version != version) {
                    info.size = mSource.getMediaItemCount();
                    info.version = version;
                }
                if (info.reloadCount > 0) {
                    info.items = mSource.getMediaItem(info.reloadStart, info.reloadCount);
                }
                executeAndWait(new UpdateContent(info));
                mVersion = version;
            }
            updateLoading(false);
        }

        public synchronized void notifyDirty() {
            mDirty = true;
            notifyAll();
        }

        public synchronized void terminate() {
            mActive = false;
            notifyAll();
        }
    }

    public long getmVersion() {
        return mVersion;
    }

}
