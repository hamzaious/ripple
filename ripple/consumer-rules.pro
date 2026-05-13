# Keep public API of the RippleEffect library so apps that consume it via XML inflation
# can still find the view class by name even with R8/ProGuard enabled.
-keep public class com.github.ripple.effect.RippleBackgroundView { *; }
-keepclassmembers class com.github.ripple.effect.RippleBackgroundView {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
