# Model e Data
-keep class com.scanproduto.model.** { *; }
-keep class com.scanproduto.data.** { *; }

# PostgreSQL JDBC Driver
-keep class org.postgresql.** { *; }
-dontwarn org.postgresql.**
-dontwarn com.sun.**
-dontwarn javax.naming.**
-dontwarn javax.xml.**
