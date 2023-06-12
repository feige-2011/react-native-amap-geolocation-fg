module.exports = {
  dependency: { platforms: { ios: { project: "lib/ios/react-native-amap-geolocation-fg.podspec" },android: { sourceDir: "lib/android" } } },
  dependencies: {
    "React-native-amap-geolocation-fg": {
      root: __dirname,
      platforms: {
       ios: { podspecPath: __dirname + "/lib/ios/react-native-amap-geolocation-fg.podspec" },
        android: {
          sourceDir: __dirname + "/lib/android",
          packageImportPath: "import cn.lyf.react.geolocation.BDGeolocationPackage;",
          packageInstance: "new BDGeolocationPackage()",
        },
      },
    },
  },
};
