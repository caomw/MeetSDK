#include "FFMediaExtractor.h"

#include "jniUtils.h"
#include "player/extractor.h"
#include "platform/autolock.h"
#include <stdio.h>
#include <dlfcn.h> // for dlopen ...
#define LOG_TAG "JNI-MediaExtractor"
#include "pplog.h"

typedef IExtractor* (*GET_EXTRACTOR_FUN)();
typedef  void (*RELEASE_EXTRACTOR_FUN)(IExtractor*);
GET_EXTRACTOR_FUN		getExtractorFun = NULL; // function to NEW ffextractor instance
RELEASE_EXTRACTOR_FUN	releaseExtractorFun = NULL; // function to DELETE ffextractor instance

static XOMediaPlayerListener* s_listener = NULL;

static pthread_mutex_t sExtractorLock;

struct fields_t {
	jfieldID    context;
	jmethodID   post_event;
};

static fields_t fields;

jobject gs_obj;     // Reference to MediaExtrctor class

/*
 * Throw an exception with the specified class and an optional message.
 *
 * If an exception is currently pending, we log a warning message and
 * clear it.
 *
 * Returns 0 if the specified exception was successfully thrown.  (Some
 * sort of exception will always be pending when this returns.)
 */
static int jniThrowException(JNIEnv* env, const char* className, const char* msg)
{
    jclass exceptionClass;

    if (env->ExceptionCheck()) {
        /* TODO: consider creating the new exception with this as "cause" */
        char buf[256];

        jthrowable exception = env->ExceptionOccurred();
        env->ExceptionClear();
    }

    exceptionClass = env->FindClass(className);
    if (exceptionClass == NULL) {
        PPLOGE("Unable to find exception class %s\n", className);
        /* ClassNotFoundException now pending */
        return -1;
    }

    int result = 0;
    if (env->ThrowNew(exceptionClass, msg) != JNI_OK) {
        PPLOGE("Failed throwing '%s' '%s'\n", className, msg);
        /* an exception, most likely OOM, will now be pending */
        result = -1;
    }

    env->DeleteLocalRef(exceptionClass);
    return result;
}

// ----------------------------------------------------------------------------

XOMediaPlayerListener::XOMediaPlayerListener(JNIEnv* env, jobject thiz, jobject weak_thiz)
{
	PPLOGI("XOMediaPlayerListener constructor");

	// Hold onto the MediaPlayer class for use in calling the static method
	// that posts events to the application thread.
	jclass clazz = env->GetObjectClass(thiz);
	if (clazz == NULL) {
		PPLOGE("Can't find android/pplive/media/player/XOMediaPlayer");
		jniThrowException(env, "java/lang/Exception", NULL);
		return;
	}
	mClass = (jclass)env->NewGlobalRef(clazz);

	// We use a weak reference so the MediaPlayer object can be garbage collected.
	// The reference is only used as a proxy for callbacks.
	mObject  = env->NewGlobalRef(weak_thiz);
}

XOMediaPlayerListener::~XOMediaPlayerListener()
{
	PPLOGI("XOMediaPlayerListener destructor");

	JNIEnv *env = getJNIEnvPP();
	env->DeleteGlobalRef(mObject);
	env->DeleteGlobalRef(mClass);
}

void XOMediaPlayerListener::notify(int msg, int ext1, int ext2)
{
	JNIEnv *env = getAttachedJNIEnv();

	if (env)
		env->CallStaticVoidMethod(mClass, fields.post_event, mObject, msg, ext1, ext2, 0);
}

// ----------------------------------------------------------------------------

static IExtractor* getMediaExtractor(JNIEnv* env, jobject thiz)
{
	AutoLock l(&sExtractorLock);
	IExtractor* e = (IExtractor*)env->GetLongField(thiz, fields.context);
	return e;
}

static IExtractor* setMediaExtractor(JNIEnv* env, jobject thiz, IExtractor* extractor)
{
	AutoLock l(&sExtractorLock);
	IExtractor* old = (IExtractor*)env->GetLongField(thiz, fields.context);
	env->SetLongField(thiz, fields.context, (int64_t)extractor);
	return old;
}

jboolean android_media_MediaExtractor_advance(JNIEnv *env, jobject thiz)
{
	IExtractor* extractor = getMediaExtractor(env, thiz);
	if (extractor == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return false;
	}

	return (extractor->advance() == OK);
}

jlong android_media_MediaExtractor_getCachedDuration(JNIEnv *env, jobject thiz)
{
	IExtractor* extractor = getMediaExtractor(env, thiz);
	if (extractor == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return UNKNOWN_ERROR;
	}

	int64_t duration = 0;
	bool eos;

	if (OK == extractor->getCachedDuration(&duration, &eos))
		return duration;

	return 0;
}

jint android_media_MediaExtractor_getSampleFlags(JNIEnv *env, jobject thiz)
{
	IExtractor* extractor = getMediaExtractor(env, thiz);
	if (extractor == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return UNKNOWN_ERROR;
	}

	uint32_t flags = 0;
	if (OK == extractor->getSampleFlags(&flags))
		return flags;

	return 0;
}

jlong android_media_MediaExtractor_getSampleTime(JNIEnv *env, jobject thiz)
{
	IExtractor* extractor = getMediaExtractor(env, thiz);
	if (extractor == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return UNKNOWN_ERROR;
	}

	int64_t usec = 0;
	if (OK != extractor->getSampleTime(&usec)) {
		PPLOGE("failed to getSampleTime()");
		return -1;
	}

	return usec;
}

jint android_media_MediaExtractor_getSampleTrackIndex(JNIEnv *env, jobject thiz)
{
	IExtractor* extractor = getMediaExtractor(env, thiz);
	if (extractor == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return UNKNOWN_ERROR;
	}

	int32_t index = -1;
	if (OK != extractor->getSampleTrackIndex(&index)) {
		PPLOGE("failed to getSampleTrackIndex()");
		return UNKNOWN_ERROR;
	}

	return index;
}

jint android_media_MediaExtractor_getTrackCount(JNIEnv *env, jobject thiz)
{
	PPLOGI("getTrackCount()");

	IExtractor* extractor = getMediaExtractor(env, thiz);
	if (extractor == NULL ) {
		jniThrowException(env, "java/lang/IllegalStateException", NULL);
		return UNKNOWN_ERROR;
	}

	int32_t count = 0;
	if (OK == extractor->getTrackCount(&count))
		return count;

	return -1;
}

int fill_bytebuffer(JNIEnv *env, jobject thiz, jobject byteBuf, uint8_t* data, uint32_t len)
{
	if (data == NULL || len == 0) {
		PPLOGE("extra data is null");
		return -1;
	}

	void *dst = env->GetDirectBufferAddress(byteBuf);

	jlong dstSize;
	jbyteArray byteArray = NULL;

    if (dst == NULL) {
        jclass byteBufClass = env->FindClass("java/nio/ByteBuffer");
        if (byteBufClass == NULL) {
			PPLOGE("failed to find class: java/nio/ByteBuffer");
			return -1;
		}

        jmethodID arrayID =
            env->GetMethodID(byteBufClass, "array", "()[B");
        if (arrayID == NULL) {
			PPLOGE("failed to GetMethodID: array");
			return -1;
		}

        byteArray = (jbyteArray)env->CallObjectMethod(byteBuf, arrayID);

        if (byteArray == NULL) {
            return -1;
        }

        jboolean isCopy;
        dst = env->GetByteArrayElements(byteArray, &isCopy);

        dstSize = env->GetArrayLength(byteArray);
    } else {
        dstSize = env->GetDirectBufferCapacity(byteBuf);
    }

	if (len > dstSize) {
		if (byteArray != NULL) {
            env->ReleaseByteArrayElements(byteArray, (jbyte *)dst, 0);
        }

		PPLOGE("dstSize is less than offset %d.%d", dstSize, len);
        return -1;
	}

	memcpy(dst, data, len);

	jclass byteBufClass = env->FindClass("java/nio/ByteBuffer");
    if (byteBufClass == NULL) {
		PPLOGE("failed to find class: java/nio/ByteBuffer");
		return -1;
	}
	jmethodID limitID = env->GetMethodID(byteBufClass, "limit", "(I)Ljava/nio/Buffer;");
    if (limitID == NULL) {
		PPLOGE("failed to GetMethodID: limit");
		return -1;
	}
	env->CallObjectMethod(byteBuf, limitID, len);

	return 0;
}

jboolean android_media_MediaExtractor_getTrackFormatNative(JNIEnv *env, jobject thiz, jint index, 
														   jobject mediaformat, jobject buf1, jobject buf2)
{
	PPLOGI("getTrackFormatNative() stream #%d", index);

	jclass clazz = env->FindClass("android/media/MediaFormat");
	if (!clazz) {
		PPLOGE("failed to get class android/media/MediaFormat");
		jniThrowException(env, "java/lang/IllegalStateException", "failed to get class android/media/MediaFormat");
		return false;
	}

	// final void setInteger(String name, int value)
	jmethodID midSetInteger = env->GetMethodID(clazz, "setInteger", "(Ljava/lang/String;I)V");
	// final void setLong (String name, long value)
	jmethodID midSetLong = env->GetMethodID(clazz, "setLong", "(Ljava/lang/String;J)V");
	// final void setFloat (String name, float value)
	jmethodID midSetFloat = env->GetMethodID(clazz, "setFloat", "(Ljava/lang/String;F)V");
	// final void setString(String name, String value)
	jmethodID midSetString = env->GetMethodID(clazz, "setString", "(Ljava/lang/String;Ljava/lang/String;)V");
	// final void setByteBuffer(String name, ByteBuffer bytes)
	jmethodID midSetByteBuffer = env->GetMethodID(clazz, "setByteBuffer", "(Ljava/lang/String;Ljava/nio/ByteBuffer;)V");

	if (!midSetInteger || !midSetLong || !midSetFloat || !midSetString || !midSetByteBuffer) {
		PPLOGE("failed to GetMethodID from class android/media/MediaFormat");
		return false;
	}

	IExtractor* extractor = getMediaExtractor(env, thiz);
	if (extractor == NULL ) {
		PPLOGE("failed to get ffextractor");
		return false;
	}

	MediaFormat native_format;
	memset(&native_format, 0, sizeof(native_format));
	status_t stat = extractor->getTrackFormat(index, &native_format);
	if (stat != OK) {
		PPLOGE("failed to getTrackFormat #%d", index);
		return false;
	}

	PPLOGI("before set mediaformat context");

	if (PPMEDIA_TYPE_VIDEO == native_format.media_type) {
		PPLOGI("begin to fill video media format: %d x %d", native_format.width, native_format.height);

		char video_mime[64] = {0};
		strcpy(video_mime, "video/");
		switch (native_format.codec_id) {
		case PPMEDIA_CODEC_ID_H264:
			strcat(video_mime, "avc");
			break;
		case PPMEDIA_CODEC_ID_HEVC:
			strcat(video_mime, "hevc");
			break;
		case PPMEDIA_CODEC_ID_MPEG4:
			strcat(video_mime, "mp4v-es");
			break;
		case PPMEDIA_CODEC_ID_H263:
			strcat(video_mime, "3gpp");
			break;
		case PPMEDIA_CODEC_ID_VP8:
			strcat(video_mime, "x-vnd.on2.vp8");
			break;
		case PPMEDIA_CODEC_ID_VP9:
			strcat(video_mime, "x-vnd.on2.vp9");
			break;
		default:
			strcat(video_mime, "unknown");
			PPLOGE("unsupported mediacodec codec id: %d", native_format.codec_id);
			break;
		}

		env->CallVoidMethod(mediaformat, midSetString, env->NewStringUTF("mime"), env->NewStringUTF(video_mime));
		env->CallVoidMethod(mediaformat, midSetInteger, env->NewStringUTF("width"), native_format.width);
		env->CallVoidMethod(mediaformat, midSetInteger, env->NewStringUTF("height"), native_format.height);
		env->CallVoidMethod(mediaformat, midSetLong, env->NewStringUTF("durationUs"), native_format.duration_us);
		env->CallVoidMethod(mediaformat, midSetFloat, env->NewStringUTF("frame-rate"), native_format.frame_rate);

		fill_bytebuffer(env, thiz, buf1, native_format.csd_0, native_format.csd_0_size);
		env->CallVoidMethod(mediaformat, midSetByteBuffer, env->NewStringUTF("csd-0"), buf1); // sps

		fill_bytebuffer(env, thiz, buf2, native_format.csd_1, native_format.csd_1_size);
		env->CallVoidMethod(mediaformat, midSetByteBuffer, env->NewStringUTF("csd-1"), buf2); // pps
	}
	else if (PPMEDIA_TYPE_AUDIO == native_format.media_type) {
		PPLOGI("begin to fill audio media format: chn %d, sample_rate %d", native_format.channels, native_format.sample_rate);

		char audio_mime[64] = {0};
		strcpy(audio_mime, "audio/");
		switch (native_format.codec_id) {
		case PPMEDIA_CODEC_ID_AAC:
			strcat(audio_mime, "mp4a-latm");
			break;
		case PPMEDIA_CODEC_ID_MP3:
			strcat(audio_mime, "mpeg");
			break;
		case PPMEDIA_CODEC_ID_AC3:
		case PPMEDIA_CODEC_ID_EAC3:
			strcat(audio_mime, "dd");
			break;
		case PPMEDIA_CODEC_ID_DTS:
			strcat(audio_mime, "dts");
			break;
		case PPMEDIA_CODEC_ID_VORBIS:
			strcat(audio_mime, "vorbis");
			break;
		case PPMEDIA_CODEC_ID_AMR_NB:
			strcat(audio_mime, "3gpp");
			break;
		case PPMEDIA_CODEC_ID_AMR_WB:
			strcat(audio_mime, "amr-wb");
			break;
		case PPMEDIA_CODEC_ID_PCM_ALAW:
			strcat(audio_mime, "g711-alaw");
			break;
		case PPMEDIA_CODEC_ID_PCM_MULAW:
			strcat(audio_mime, "g711-mlaw");
			break;
		default:
			strcat(audio_mime, "unknown");
			PPLOGE("unsupported mediacodec codec id: %d", native_format.codec_id);
			break;
		}

		env->CallVoidMethod(mediaformat, midSetString, env->NewStringUTF("mime"), env->NewStringUTF(audio_mime));
		env->CallVoidMethod(mediaformat, midSetInteger, env->NewStringUTF("channel-count"), native_format.channels);
		env->CallVoidMethod(mediaformat, midSetInteger, env->NewStringUTF("sample-rate"), native_format.sample_rate);
		env->CallVoidMethod(mediaformat, midSetInteger, env->NewStringUTF("bitrate"), native_format.bitrate);
		env->CallVoidMethod(mediaformat, midSetLong, env->NewStringUTF("durationUs"), native_format.duration_us);
		fill_bytebuffer(env, thiz, buf1, native_format.csd_0, native_format.csd_0_size);
		env->CallVoidMethod(mediaformat, midSetByteBuffer, env->NewStringUTF("csd-0"), buf1);
	}
	else {
		PPLOGW("unknown media type: %d" + native_format.media_type);
	}

	PPLOGI("getTrackFormatNative #%d all done!", index);

	return true;
}

jboolean android_media_MediaExtractor_hasCachedReachedEndOfStream(JNIEnv *env, jobject thiz)
{
	IExtractor* extractor = getMediaExtractor(env, thiz);
	if (extractor == NULL ) {
		PPLOGE("failed to get ffextractor");
		return false;
	}

	// todo
	return false;
}

jint android_media_MediaExtractor_readSampleData(JNIEnv *env, jobject thiz, jobject byteBuf, jint offset)
{
	PPLOGD("readSampleData()");

	void *dst = env->GetDirectBufferAddress(byteBuf);

	jlong dstSize;
	jbyteArray byteArray = NULL;

    if (dst == NULL) {
        jclass byteBufClass = env->FindClass("java/nio/ByteBuffer");
        if (byteBufClass == NULL) {
			PPLOGE("failed to find class: java/nio/ByteBuffer");
			return INVALID_OPERATION;
		}

        jmethodID arrayID =
            env->GetMethodID(byteBufClass, "array", "()[B");
        if (arrayID == NULL) {
			PPLOGE("failed to GetMethodID: array");
			return INVALID_OPERATION;
		}

        byteArray = (jbyteArray)env->CallObjectMethod(byteBuf, arrayID);

        if (byteArray == NULL) {
            return INVALID_OPERATION;
        }

        jboolean isCopy;
        dst = env->GetByteArrayElements(byteArray, &isCopy);

        dstSize = env->GetArrayLength(byteArray);
    } else {
        dstSize = env->GetDirectBufferCapacity(byteBuf);
    }

    if (dstSize < offset) {
        if (byteArray != NULL) {
            env->ReleaseByteArrayElements(byteArray, (jbyte *)dst, 0);
        }

		PPLOGE("dstSize is less than offset %d.%d", dstSize, offset);
        return -ERANGE;
    }

	IExtractor* extractor = getMediaExtractor(env, thiz);
	if (extractor == NULL ) {
		PPLOGE("failed to get ffextractor");
		return INVALID_OPERATION;
	}

	int sample_size = 0;
	status_t err = extractor->readSampleData((unsigned char *)dst, &sample_size);

    if (byteArray != NULL) {
        env->ReleaseByteArrayElements(byteArray, (jbyte *)dst, 0);
    }

	if (err == READ_EOF) {
		PPLOGI("find eof");
		return -1;
	}
	else if (err != OK) {
		PPLOGE("failed to call readSampleData() %d", err);
        return -1;
    }

	return sample_size;
}

void android_media_MediaExtractor_stop(JNIEnv *env, jobject thiz)
{
	PPLOGI("stop");

	IExtractor* extractor = getMediaExtractor(env, thiz);
	if (extractor == NULL ) {
		PPLOGE("failed to get ffextractor");
		jniThrowException(env, "java/lang/IllegalStateException", "failed to get ffextractor");
		return;
	}

	if (OK != extractor->stop()) {
		PPLOGE("failed to stop");
		jniThrowException(env, "java/lang/IllegalStateException", "failed to stop");
	}
}

void android_media_MediaExtractor_release(JNIEnv *env, jobject thiz)
{
	PPLOGI("release()");

	IExtractor* extractor = setMediaExtractor(env, thiz, NULL);
	releaseExtractorFun(extractor);

	if (s_listener) {
		delete s_listener;
		s_listener = NULL;
	}

	pthread_mutex_destroy(&sExtractorLock);
	PPLOGI("release done!");
}

void android_media_MediaExtractor_seekTo(JNIEnv *env, jobject thiz, jlong timeUs, jint mode)
{
	PPLOGI("seekTo %lld", timeUs);
	IExtractor* extractor = getMediaExtractor(env, thiz);
	if (extractor == NULL ) {
		PPLOGE("failed to get ffextractor");
		jniThrowException(env, "java/lang/IllegalStateException", "failed to get ffextractor");
		return;
	}

	if (OK != extractor->seekTo(timeUs, mode)) {
		PPLOGE("failed to seek");
		jniThrowException(env, "java/lang/IllegalStateException", "failed to seek");
	}
}

void android_media_MediaExtractor_selectTrack(JNIEnv *env, jobject thiz, jint index)
{
	IExtractor* extractor = getMediaExtractor(env, thiz);
	if (extractor == NULL ) {
		PPLOGE("failed to get ffextractor");
		return;
	}

	extractor->selectTrack(index);
}

void android_media_MediaExtractor_setDataSource(JNIEnv *env, jobject thiz, jstring path)
{
	IExtractor* extractor = getMediaExtractor(env, thiz);
	if (extractor == NULL ) {
		PPLOGE("failed to get ffextractor");
		return;
	}

	if (path == NULL) {
		jniThrowException(env, "java/lang/Exception", "Path is NULL.");
		PPLOGE("Path is NULL");
		return;
	}

	const char *pathStr = env->GetStringUTFChars(path, NULL);
	if (pathStr == NULL) {  // Out of memory
		PPLOGE("GetStringUTFChars: Out of memory");
		return;
	}

	PPLOGI("setDataSource: %s", pathStr);

	status_t ret = extractor->setDataSource(pathStr);
	if (ret != OK) {
		jniThrowException(env, "java/lang/Exception", "failed to open media");
		return;
	}

	env->ReleaseStringUTFChars(path, pathStr);
}

void android_media_MediaExtractor_unselectTrack(JNIEnv *env, jobject thiz, jint index)
{
	IExtractor* extractor = getMediaExtractor(env, thiz);
	if (extractor == NULL ) {
		PPLOGE("failed to get ffextractor");
		return;
	}

	extractor->unselectTrack(index);
}

// static function called when sdk init
jboolean android_media_MediaExtractor_init(JNIEnv *env, jobject thiz)
{
	jclass clazz = env->FindClass("android/pplive/media/player/FFMediaExtractor");
	if (clazz == NULL) {
		PPLOGE("Can't find android/pplive/media/player/FFMediaExtractor");
		return false;
	}

	fields.context = env->GetFieldID(clazz, "mNativeContext", "J");
	if (fields.context == NULL) {
		PPLOGE("Can't find FFMediaExtractor.mNativeContext");
		return false;
	}

	jclass clazz2 = env->FindClass("android/pplive/media/player/XOMediaPlayer");
	if (clazz2 == NULL) {
		PPLOGE("Can't find android/pplive/media/player/XOMediaPlayer");
		return false;
	}
	fields.post_event = env->GetStaticMethodID(clazz2, "postEventFromNative",
			"(Ljava/lang/Object;IIILjava/lang/Object;)V");
	if (fields.post_event == NULL)
		jniThrowException(env, "java/lang/RuntimeException", "Can't find XOMediaPlayer.postEventFromNative");

	return true;
}

// called when new FFExtractor()
void android_media_MediaExtractor_setup(JNIEnv *env, jobject thiz, jobject weak_this)
{
	pthread_mutex_init(&sExtractorLock, NULL);

	if (NULL == getExtractorFun) {
		jniThrowException(env, "java/lang/RuntimeException", "getExtractorFun is null.");
		return;
	}

	IExtractor* extractor = getExtractorFun();
	if (extractor == NULL) {
		jniThrowException(env, "java/lang/RuntimeException", "Create IExtractor failed.");
		return;
	}

	// create new listener and give it to MediaPlayer
	s_listener = new XOMediaPlayerListener(env, thiz, weak_this);
	//IExtractor takes responsibility to release listener.
	extractor->setListener(s_listener);

	setMediaExtractor(env, thiz, extractor);
}

static JNINativeMethod gExtractorMethods[] = {
	{"advance",       "()Z",		(void *)android_media_MediaExtractor_advance},
	{"getCachedDuration",       "()J",		(void *)android_media_MediaExtractor_getCachedDuration},
	{"getSampleFlags",       "()I",		(void *)android_media_MediaExtractor_getSampleFlags},
	{"getSampleTime",       "()J",		(void *)android_media_MediaExtractor_getSampleTime},
	{"getSampleTrackIndex",       "()I",		(void *)android_media_MediaExtractor_getSampleTrackIndex},
	{"getTrackCount",       "()I",		(void *)android_media_MediaExtractor_getTrackCount},
	{"getTrackFormatNative",       "(ILandroid/media/MediaFormat;Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)Z",
		(void *)android_media_MediaExtractor_getTrackFormatNative},
	{"hasCachedReachedEndOfStream",       "()Z",		(void *)android_media_MediaExtractor_hasCachedReachedEndOfStream},
	{"readSampleData",       "(Ljava/nio/ByteBuffer;I)I",		(void *)android_media_MediaExtractor_readSampleData},

	{"release",       "()V",		(void *)android_media_MediaExtractor_release},
	{"stop",       "()V",		(void *)android_media_MediaExtractor_stop},
	{"seekTo",       "(JI)V",		(void *)android_media_MediaExtractor_seekTo},
	{"selectTrack",       "(I)V",		(void *)android_media_MediaExtractor_selectTrack},
	{"setDataSource",       "(Ljava/lang/String;)V",		(void *)android_media_MediaExtractor_setDataSource},
	{"unselectTrack",       "(I)V",		(void *)android_media_MediaExtractor_unselectTrack},
	{"native_init",       "()Z",		(void *)android_media_MediaExtractor_init},
	{"setup",       "(Ljava/lang/Object;)V",		(void *)android_media_MediaExtractor_setup},
};

bool setup_extractor(void *so_handle)
{
	getExtractorFun = (GET_EXTRACTOR_FUN)dlsym(so_handle, "getExtractor");
	if (getExtractorFun == NULL) {
		PPLOGE("Init getExtractorFun() failed: %s", dlerror());
		return false;
	}

	releaseExtractorFun = (RELEASE_EXTRACTOR_FUN)dlsym(so_handle, "releaseExtractor");
	if (releaseExtractorFun == NULL) {
		PPLOGE("Init releaseExtractorFun() failed: %s", dlerror());
		return false;
	}

	return true;
}

int register_android_media_MediaExtractor(JNIEnv *env)
{
	const char* className = "android/pplive/media/player/FFMediaExtractor";
	jclass clazz;

	PPLOGD("Registering %s natives", className);
	clazz = env->FindClass(className);
	if (clazz == NULL) {
		PPLOGE("Native registration unable to find class '%s'", className);
		return -1;
	}

	int result = 0;
	if (env->RegisterNatives(clazz, gExtractorMethods, NELEM(gExtractorMethods)) < 0) {
		PPLOGE("RegisterNatives failed for '%s'", className);
		result = -1;
	}

	env->DeleteLocalRef(clazz);
	return result;
}


