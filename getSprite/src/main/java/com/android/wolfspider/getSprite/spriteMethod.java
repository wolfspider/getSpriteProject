package com.android.wolfspider.getSprite;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11Ext;

/**
 * Created by Jesse on 8/2/13.
 */
public class spriteMethod extends spriteDef{

    // The OpenGL ES texture handle to draw.
    private int mTextureName;
    // The id of the original resource that mTextureName is based on.
    private int mResourceId;
    // If drawing with verts or VBO verts, the grid object defining those verts.
    private zoneGrid mGrid;

    public spriteMethod(int resourceId) {
        super();
        mResourceId = resourceId;
    }

    public void setTextureName(int name) {
        mTextureName = name;
    }

    public int getTextureName() {
        return mTextureName;
    }

    public void setResourceId(int id) {
        mResourceId = id;
    }

    public int getResourceId() {
        return mResourceId;
    }

    public void setGrid(zoneGrid grid) {
        mGrid = grid;
    }

    public zoneGrid getGrid() {
        return mGrid;
    }

    public void draw(GL10 gl) {
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureName);

        if (mGrid == null) {
            // Draw using the DrawTexture extension.
            ((GL11Ext) gl).glDrawTexfOES(x, y, z, width, height);
        } else {
            // Draw using verts or VBO verts.
            gl.glPushMatrix();
            gl.glLoadIdentity();
            gl.glTranslatef(
                    x,
                    y,
                    z);

            mGrid.draw(gl, true, false);

            gl.glPopMatrix();
        }
    }
}
