# OpenGL Android Video Effects

This fork is a collection of forks from the original creator. 

We have been using ffmpeg from java-cpp in android for recording square videos. It is excellent library, however we faced issues related to threads, ffmpeg loading, using realm and webrtc alongside it, and a very bloated apk.
Though we have sorted all those issues out, we would like a simpler approach natively supported by android. 

We will try to maintain the repository as best we can. All pull requests are welcome. 
Though bugs will only be solved that we face in our projects.

The intention is to sort out the edge cases and make it available as a library project.

### Using in your project

1. Include jitpack.io maven repo

```
    repositories {
        maven { url "https://jitpack.io" }
    }

```

2. Add dependency to project

```
    dependencies {
        implementation 'com.android.support:appcompat-v7:27.1.0'
    }

```

3. Usage

```
    private void startPicker() {
            new MediaPicker.Builder()
                    .setMediaType(MediaType.VIDEO)
                    .withGallery(Boolean.valueOf("true"))
                    .withCamera(Boolean.valueOf("true"))
                    .withCameraType(ScaleType.SCALE_CROP_CENTER)
                    .withCameraFront(Boolean.valueOf("true"))
                    .withFlash(Boolean.valueOf("true"))
                    .withMaxPick(Integer.parseInt("2"))
                    .startActivity(this);
        }
    }
    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Result result = MediaPicker.onActivityResult(requestCode, resultCode, data);
        if (result != null) {
            for (String file: result.files) {
                Log.d(TAG, "file: picked: " + file);
            }
        }
    }

```