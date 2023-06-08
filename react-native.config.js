module.exports = {
  dependency: { platforms: { android: { sourceDir: "lib/android" } } },
  dependencies: {
    "React-native-amap-geolocation-fg": {
      root: __dirname,
      platforms: {
        android: {
          sourceDir: __dirname + "/lib/android",
          packageImportPath: "import cn.lyf.react.geolocation.BDGeolocationPackage;",
          packageInstance: "new BDGeolocationPackage()",
        },
      },
    },
  },
};
