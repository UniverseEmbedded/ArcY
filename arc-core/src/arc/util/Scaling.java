package arc.util;

import arc.math.geom.Vec2;

/**
 * Various scaling types for fitting one rectangle into another.
 * @author Nathan Sweet
 */
public enum Scaling{
    /**
     * Scales the source to fit the target while keeping the same aspect ratio. This may cause the source to be smaller than the
     * target in one direction.
     */
    fit,
    /**
     * Scales the source to fit the target if it is larger, otherwise does not scale.
     */
    bounded,
    /**
     * Scales the source to fill the target while keeping the same aspect ratio. This may cause the source to be larger than the
     * target in one direction.
     */
    fill,
    /**
     * Scales the source to fill the target in the x direction while keeping the same aspect ratio. This may cause the source to be
     * smaller or larger than the target in the y direction.
     */
    fillX,
    /**
     * Scales the source to fill the target in the y direction while keeping the same aspect ratio. This may cause the source to be
     * smaller or larger than the target in the x direction.
     */
    fillY,
    /** Scales the source to fill the target. This may cause the source to not keep the same aspect ratio. */
    stretch,
    /**
     * Scales the source to fill the target in the x direction, without changing the y direction. This may cause the source to not
     * keep the same aspect ratio.
     */
    stretchX,
    /**
     * Scales the source to fill the target in the y direction, without changing the x direction. This may cause the source to not
     * keep the same aspect ratio.
     */
    stretchY,
    /** The source is not scaled. */
    none;

    private static final ThreadLocal<Vec2> temp = ThreadLocal.withInitial(Vec2::new);

    /**
     * Returns the size of the source scaled to the target. Note the same Vec2 instance is always returned and should never be
     * cached.
     */
    public Vec2 apply(float sourceWidth, float sourceHeight, float targetWidth, float targetHeight){
        Vec2 result = temp.get();
        switch(this){
            case fit:{
                float targetRatio = targetHeight / targetWidth;
                float sourceRatio = sourceHeight / sourceWidth;
                float scale = targetRatio > sourceRatio ? targetWidth / sourceWidth : targetHeight / sourceHeight;
                result.x = sourceWidth * scale;
                result.y = sourceHeight * scale;
                break;
            }
            case fill:{
                float targetRatio = targetHeight / targetWidth;
                float sourceRatio = sourceHeight / sourceWidth;
                float scale = targetRatio < sourceRatio ? targetWidth / sourceWidth : targetHeight / sourceHeight;
                result.x = sourceWidth * scale;
                result.y = sourceHeight * scale;
                break;
            }
            case fillX:{
                float scale = targetWidth / sourceWidth;
                result.x = sourceWidth * scale;
                result.y = sourceHeight * scale;
                break;
            }
            case fillY:{
                float scale = targetHeight / sourceHeight;
                result.x = sourceWidth * scale;
                result.y = sourceHeight * scale;
                break;
            }
            case stretch:
                result.x = targetWidth;
                result.y = targetHeight;
                break;
            case stretchX:
                result.x = targetWidth;
                result.y = sourceHeight;
                break;
            case stretchY:
                result.x = sourceWidth;
                result.y = targetHeight;
                break;
            case bounded:
                if(sourceHeight > targetHeight || sourceWidth > targetWidth){
                    return fit.apply(sourceWidth, sourceHeight, targetWidth, targetHeight);
                }else{
                    return none.apply(sourceWidth, sourceHeight, targetWidth, targetHeight);
                }
            case none:
                result.x = sourceWidth;
                result.y = sourceHeight;
                break;
        }
        return result;
    }
}
