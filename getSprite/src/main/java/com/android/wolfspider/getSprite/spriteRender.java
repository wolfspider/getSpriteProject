package com.android.wolfspider.getSprite;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import android.opengl.GLES20;
import android.util.Log;

/**
 * Created by Jesse on 8/2/13.
 */
public class spriteRender implements spriteSurface.Renderer{

    // Specifies the format our textures should be converted to upon load.
    private static BitmapFactory.Options sBitmapOptions
            = new BitmapFactory.Options();
    // An array of things to draw every frame.
    private spriteMethod[] mSprites;
    // Pre-allocated arrays to use at runtime so that allocation during the
    // test can be avoided.
    private int[] mTextureNameWorkspace;
    private int[] mCropWorkspace;
    // A reference to the application context.
    private Context mContext;
    // Determines the use of vertex arrays.
    private boolean mUseVerts;
    // Determines the use of vertex buffer objects.
    private boolean mUseHardwareBuffers;

    public spriteRender(Context context) {
        // Pre-allocate and store these objects so we can use them at runtime
        // without allocating memory mid-frame.
        mTextureNameWorkspace = new int[1];
        mCropWorkspace = new int[4];

        // Set our bitmaps to 16-bit, 565 format.
        sBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;

        mContext = context;
    }

    public int[] getConfigSpec() {
        // We don't need a depth buffer, and don't care about our
        // color depth.
        int[] configSpec = { EGL10.EGL_DEPTH_SIZE, 0, EGL10.EGL_NONE };
        return configSpec;
    }

    public void setSprites(spriteMethod[] sprites) {
        mSprites = sprites;
    }

    /**
     * Changes the vertex mode used for drawing.
     * @param useVerts  Specifies whether to use a vertex array.  If false, the
     *     DrawTexture extension is used.
     * @param useHardwareBuffers  Specifies whether to store vertex arrays in
     *     main memory or on the graphics card.  Ignored if useVerts is false.
     */
    public void setVertMode(boolean useVerts, boolean useHardwareBuffers) {
        mUseVerts = useVerts;
        mUseHardwareBuffers = useVerts ? useHardwareBuffers : false;
    }

    /** Draws the sprites. */
    public void drawFrame(GL10 gl) {
        if (mSprites != null) {

            gl.glMatrixMode(GL10.GL_MODELVIEW);

            if (mUseVerts) {
                zoneGrid.beginDrawing(gl, true, false);
            }

            for (int x = 0; x < mSprites.length; x++) {
                mSprites[x].draw(gl);
            }

            if (mUseVerts) {
                zoneGrid.endDrawing(gl);
            }


        }
    }

    /* Called when the size of the window changes. */
    public void sizeChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);

        /*
         * Set our projection matrix. This doesn't have to be done each time we
         * draw, but usually a new projection needs to be set when the viewport
         * is resized.
         */
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrthof(0.0f, width, 0.0f, height, 0.0f, 1.0f);

        gl.glShadeModel(GL10.GL_FLAT);
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl.glColor4x(0x10000, 0x10000, 0x10000, 0x10000);
        gl.glEnable(GL10.GL_TEXTURE_2D);
    }

    /**
     * Called whenever the surface is created.  This happens at startup, and
     * may be called again at runtime if the device context is lost (the screen
     * goes to sleep, etc).  This function must fill the contents of vram with
     * texture data and (when using VBOs) hardware vertex arrays.
     */
    public void surfaceCreated(GL10 gl) {
         /*
         * Some one-time OpenGL initialization can be made here probably based
         * on features of this particular context
         */
        //GLES20.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1);
        //gl.glShadeModel(GL10.GL_FLAT);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        /*
         * By default, OpenGL enables features that improve quality but reduce
         * performance. One might want to tweak that especially on software
         * renderer.
         */
        GLES20.glDisable(GLES20.GL_DITHER);
        //GLES20.glDisable(GL10.GL_LIGHTING);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mSprites != null) {

            // If we are using hardware buffers and the screen lost context
            // then the buffer indexes that we recorded previously are now
            // invalid.  Forget them here and recreate them below.
            if (mUseHardwareBuffers) {
                for (int x = 0; x < mSprites.length; x++) {
                    // Ditch old buffer indexes.
                    mSprites[x].getGrid().invalidateHardwareBuffers();
                }
            }

            // Load our texture and set its texture name on all sprites.

            // To keep this sample simple we will assume that sprites that share
            // the same texture are grouped together in our sprite list. A real
            // app would probably have another level of texture management,
            // like a texture hash.

            int lastLoadedResource = -1;
            int lastTextureId = -1;

            for (int x = 0; x < mSprites.length; x++) {
                int resource = mSprites[x].getResourceId();
                if (resource != lastLoadedResource) {
                    lastTextureId = loadBitmap(mContext, gl, resource);
                    lastLoadedResource = resource;
                }
                mSprites[x].setTextureName(lastTextureId);
                if (mUseHardwareBuffers) {
                    zoneGrid currentGrid = mSprites[x].getGrid();
                    if (!currentGrid.usingHardwareBuffers()) {
                        currentGrid.generateHardwareBuffers(gl);
                    }
                    mSprites[x].getGrid().generateHardwareBuffers(gl);
                }
            }
        }
    }

    /**
     * Called when the rendering thread shuts down.  This is a good place to
     * release OpenGL ES resources.
     * @param gl
     */
    public void shutdown(GL10 gl) {
        if (mSprites != null) {

            int lastFreedResource = -1;
            int[] textureToDelete = new int[1];

            for (int x = 0; x < mSprites.length; x++) {
                int resource = mSprites[x].getResourceId();
                if (resource != lastFreedResource) {
                    textureToDelete[0] = mSprites[x].getTextureName();
                    gl.glDeleteTextures(1, textureToDelete, 0);
                    mSprites[x].setTextureName(0);
                }
                if (mUseHardwareBuffers) {
                    mSprites[x].getGrid().releaseHardwareBuffers(gl);
                }
            }
        }
    }

    /**
     * Loads a bitmap into OpenGL and sets up the common parameters for
     * 2D texture maps.
     */
    protected int loadBitmap(Context context, GL10 gl, int resourceId) {
        int textureName = -1;
        if (context != null && gl != null) {

            GLES20.glGenTextures(1, mTextureNameWorkspace, 0);

            textureName = mTextureNameWorkspace[0];
            //gl.glBindTexture(GL10.GL_TEXTURE_2D, textureName);
            //...and bind it to our array
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureName);

            //gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
            //gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

            //gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
            //gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

            //Create Nearest Filtered Texture
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            //Different possible texture parameters, e.g. GLES20.GL_CLAMP_TO_EDGE
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);


            //gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_REPLACE);

            InputStream is = context.getResources().openRawResource(resourceId);
            Bitmap bitmap;
            try {
                bitmap = BitmapFactory.decodeStream(is, null, sBitmapOptions);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore.
                }
            }

            //GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            mCropWorkspace[0] = 0;
            mCropWorkspace[1] = bitmap.getHeight();
            mCropWorkspace[2] = bitmap.getWidth();
            mCropWorkspace[3] = -bitmap.getHeight();

            bitmap.recycle();

            /*
            ((GL11) gl).glTexParameteriv(GL10.GL_TEXTURE_2D,
                    GL11Ext.GL_TEXTURE_CROP_RECT_OES, mCropWorkspace, 0);*/

           // GLES20.glTexParameteriv(GLES20.GL_TEXTURE_2D,GL11Ext.GL_TEXTURE_CROP_RECT_OES, mCropWorkspace, 0);


            int error = GLES20.glGetError();
            if (error != GLES20.GL_NO_ERROR) {
                Log.e("spriteRender", "Texture Load GLError: " + error);
            }

        }

        return textureName;
    }
}
