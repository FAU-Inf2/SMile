package com.fsck.k9.activity.misc;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.fsck.k9.helper.Contacts;
import com.fsck.k9.mail.Address;
import com.fsck.k9.view.QuickContactBadge;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContactPictureLoader {
    /**
     * Resize the pictures to the following value (device-independent pixels).
     */
    private static final int PICTURE_SIZE = 40;

    /**
     * Pattern to extract the letter to be displayed as fallback image.
     */
    private static final Pattern EXTRACT_LETTER_PATTERN = Pattern.compile("[a-zA-Z]");

    /**
     * Letter to use when {@link #EXTRACT_LETTER_PATTERN} couldn't find a match.
     */
    private static final String FALLBACK_CONTACT_LETTER = "?";

    private Context mContext;
    private Resources mResources;
    private int mPictureSizeInPx;
    private int mDefaultBackgroundColor;

    /**
     * LRU cache of contact pictures.
     */
    private final LruCache<Address, Bitmap> mBitmapCache;

    /**
     * @see <a href="http://developer.android.com/design/style/color.html">Color palette used</a>
     */
    private final static int CONTACT_DUMMY_COLORS_ARGB[] = {
        0xff33B5E5,
        0xffAA66CC,
        0xff99CC00,
        0xffFFBB33,
        0xffFF4444,
        0xff0099CC,
        0xff9933CC,
        0xff669900,
        0xffFF8800,
        0xffCC0000
    };

    /**
     * Constructor.
     *
     * @param context
     *         A {@link Context} instance.
     * @param defaultBackgroundColor
     *         The ARGB value to be used as background color for the fallback picture. {@code 0} to
     *         use a dynamically calculated background color.
     */
    public ContactPictureLoader(Context context, int defaultBackgroundColor) {
        mContext = context.getApplicationContext();
        mResources = mContext.getResources();

        float scale = mResources.getDisplayMetrics().density;
        mPictureSizeInPx = (int) (PICTURE_SIZE * scale);

        mDefaultBackgroundColor = defaultBackgroundColor;

        ActivityManager activityManager =
                (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        int memClass = activityManager.getMemoryClass();

        // Use 1/16th of the available memory for this memory cache.
        final int cacheSize = 1024 * 1024 * memClass / 16;

        mBitmapCache = new LruCache<Address, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(Address key, Bitmap bitmap) {
                // The cache size will be measured in bytes rather than number of items.
                return bitmap.getByteCount();
            }
        };
    }

    /**
     * Load a contact picture and display it using the supplied {@link QuickContactBadge} instance.
     *
     * <p>
     * If a picture is found in the cache, it is displayed in the {@code QuickContactBadge}
     * immediately. Otherwise a {@link ContactPictureRetrievalTask} is started to try to load the
     * contact picture in a background thread. Depending on the result the contact picture or a
     * fallback picture is then stored in the bitmap cache.
     * </p>
     *
     * @param address
     *         The {@link Address} instance holding the email address that is used to search the
     *         contacts database.
     * @param badge
     *         The {@code QuickContactBadge} instance to receive the picture.
     *
     * @see #mBitmapCache
     * @see #calculateFallbackBitmap(Address)
     */
    public void loadContactPicture(Address address, QuickContactBadge badge) {
        Bitmap bitmap = getBitmapFromCache(address);
        if (bitmap != null) {
            // The picture was found in the bitmap cache
            badge.setImageBitmap(bitmap);
        } else if (cancelPotentialWork(address, badge)) {
            // Query the contacts database in a background thread and try to load the contact
            // picture, if there is one.
            ContactPictureRetrievalTask task = new ContactPictureRetrievalTask(mContext, badge, address);
            AsyncDrawable asyncDrawable = new AsyncDrawable(mResources,
                    calculateFallbackBitmap(address), task);
            badge.setImageDrawable(asyncDrawable);
            try {
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } catch (RejectedExecutionException e) {
                // We flooded the thread pool queue... use a fallback picture
                badge.setImageBitmap(calculateFallbackBitmap(address));
            }
        }
    }

    private int calcUnknownContactColor(Address address) {
        if (mDefaultBackgroundColor != 0) {
            return mDefaultBackgroundColor;
        }

        int val = address.hashCode();
        int colorIndex = (val & Integer.MAX_VALUE) % CONTACT_DUMMY_COLORS_ARGB.length;
        return CONTACT_DUMMY_COLORS_ARGB[colorIndex];
    }

    private String calcUnknownContactLetter(Address address) {
        String letter = null;
        String personal = address.getPersonal();
        String str = (personal != null) ? personal : address.getAddress();

        Matcher m = EXTRACT_LETTER_PATTERN.matcher(str);
        if (m.find()) {
            letter = m.group(0).toUpperCase(Locale.US);
        }

        return (TextUtils.isEmpty(letter)) ?
                FALLBACK_CONTACT_LETTER : letter.substring(0, 1);
    }

    /**
     * Calculates a bitmap with a color and a capital letter for contacts without picture.
     */
    private Bitmap calculateFallbackBitmap(Address address) {
        int rgb = calcUnknownContactColor(address);
        String letter = calcUnknownContactLetter(address);
        return generateCircleBitmap(rgb, 40, letter);
    }

    /**
     * Create round, coloured bitmap with text embedded.
     * @param circleColor The color to use.
     * @param diameterDP The diameter of the circle.
     * @param text The text to embed.
     * @return Bitmap showing a text.
     */
    public Bitmap generateCircleBitmap(int circleColor, float diameterDP, String text) {
        /**
         *
         * http://stackoverflow.com/questions/31168636/rounded-quickcontactbadge-with-text
         */
        final int textColor = 0xffffffff;

        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float diameterPixels = diameterDP * (metrics.densityDpi / 160f);
        float radiusPixels = diameterPixels / 2;

        // Create the bitmap
        Bitmap output = Bitmap.createBitmap((int) diameterPixels, (int) diameterPixels, Bitmap.Config.ARGB_8888);

        // Create the canvas to draw on
        Canvas canvas = new Canvas(output);
        canvas.drawARGB(0, 0, 0, 0);

        // Draw the circle
        final Paint paintC = new Paint();
        paintC.setAntiAlias(true);
        paintC.setColor(circleColor);
        canvas.drawCircle(radiusPixels, radiusPixels, radiusPixels, paintC);

        // Draw the text
        if (text != null && text.length() > 0) {
            final Paint paintT = new Paint();
            paintT.setColor(textColor);
            paintT.setAntiAlias(true);
            paintT.setTextSize(radiusPixels * 2);
            paintT.setTypeface(Typeface.SANS_SERIF);
            final Rect textBounds = new Rect();
            paintT.getTextBounds(text, 0, text.length(), textBounds);
            canvas.drawText(text, radiusPixels - textBounds.exactCenterX(), radiusPixels - textBounds.exactCenterY(), paintT);
        }

        return output;
    }

    private void addBitmapToCache(Address key, Bitmap bitmap) {
        if (getBitmapFromCache(key) == null) {
            mBitmapCache.put(key, bitmap);
        }
    }

    private Bitmap getBitmapFromCache(Address key) {
        return mBitmapCache.get(key);
    }

    /**
     * Checks if a {@code ContactPictureRetrievalTask} was already created to load the contact
     * picture for the supplied {@code Address}.
     *
     * @param address
     *         The {@link Address} instance holding the email address that is used to search the
     *         contacts database.
     * @param badge
     *         The {@code QuickContactBadge} instance that will receive the picture.
     *
     * @return {@code true}, if the contact picture should be loaded in a background thread.
     *         {@code false}, if another {@link ContactPictureRetrievalTask} was already scheduled
     *         to load that contact picture.
     */
    private boolean cancelPotentialWork(Address address, QuickContactBadge badge) {
        final ContactPictureRetrievalTask task = getContactPictureRetrievalTask(badge);

        if (task != null && address != null) {
            if (!address.equals(task.getAddress())) {
                // Cancel previous task
                task.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }

        // No task associated with the QuickContactBadge, or an existing task was cancelled
        return true;
    }

    private ContactPictureRetrievalTask getContactPictureRetrievalTask(QuickContactBadge badge) {
        if (badge != null) {
           Drawable drawable = badge.getDrawable();
           if (drawable instanceof AsyncDrawable) {
               AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
               return asyncDrawable.getContactPictureRetrievalTask();
           }
        }

        return null;
    }

    /**
     * Load a contact picture in a background thread.
     */
    class ContactPictureRetrievalTask extends AsyncTask<Void, Void, Bitmap> {
        private final WeakReference<QuickContactBadge> mQuickContactBadgeReference;
        private final Address mAddress;
        private final Context mContext;
        private final Contacts mContactsHelper;
        private final ContentResolver mContentResolver;

        ContactPictureRetrievalTask(final Context context, QuickContactBadge badge, Address address) {
            this.mContext = context;
            this.mContactsHelper = Contacts.getInstance(mContext);
            this.mContentResolver = mContext.getContentResolver();
            mQuickContactBadgeReference = new WeakReference<>(badge);
            mAddress = new Address(address);
        }

        public Address getAddress() {
            return mAddress;
        }

        @Override
        protected Bitmap doInBackground(Void... args) {
            final String email = mAddress.getAddress();
            final Uri photoUri = mContactsHelper.getPhotoUri(email);
            Bitmap bitmap = null;
            if (photoUri != null) {
                try {
                    InputStream stream = mContentResolver.openInputStream(photoUri);
                    if (stream != null) {
                        try {
                            Bitmap tempBitmap = BitmapFactory.decodeStream(stream);
                            if (tempBitmap != null) {
                                bitmap = Bitmap.createScaledBitmap(tempBitmap, mPictureSizeInPx,
                                        mPictureSizeInPx, true);
                                if (tempBitmap != bitmap) {
                                    tempBitmap.recycle();
                                }
                            }
                        } finally {
                            try { stream.close(); } catch (IOException e) { /* ignore */ }
                        }
                    }
                } catch (FileNotFoundException e) {
                    /* ignore */
                }

            }

            if (bitmap == null) {
                bitmap = calculateFallbackBitmap(mAddress);
            }
            // Save the picture of the contact with that email address in the bitmap cache
            addBitmapToCache(mAddress, bitmap);

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (mQuickContactBadgeReference != null) {
                QuickContactBadge badge = mQuickContactBadgeReference.get();
                if (badge != null && getContactPictureRetrievalTask(badge) == this) {
                    badge.setImageBitmap(bitmap);
                }
            }
        }
    }

    /**
     * {@code Drawable} subclass that stores a reference to the {@link ContactPictureRetrievalTask}
     * that is trying to load the contact picture.
     *
     * <p>
     * The reference is used by {@link ContactPictureLoader#cancelPotentialWork(Address,
     * QuickContactBadge)} to find out if the contact picture is already being loaded by a
     * {@code ContactPictureRetrievalTask}.
     * </p>
     */
    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<ContactPictureRetrievalTask> mAsyncTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap, ContactPictureRetrievalTask task) {
            super(res, bitmap);
            mAsyncTaskReference = new WeakReference<>(task);
        }

        public ContactPictureRetrievalTask getContactPictureRetrievalTask() {
            return mAsyncTaskReference.get();
        }
    }
}
