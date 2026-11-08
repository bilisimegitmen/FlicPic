# Add project specific ProGuard rules here.

# Coil image loading
-dontwarn coil.**
-keep class coil.** { *; }

# Compose stability
-dontwarn androidx.compose.**
