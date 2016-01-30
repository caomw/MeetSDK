#ifndef _RENDER_BASE_H_
#define _RENDER_BASE_H_

#include <stdint.h>
#include <stdio.h>

struct AVFrame;

class RenderBase
{
public:
	RenderBase()
		:mSurface(NULL), mWidth(0), mHeight(0), mAveScaleTimeMs(0)
	{
	}

	virtual ~RenderBase(void){}

	// ��ʼrender.
	virtual bool init_render(void* ctx, int w, int h, int pix_fmt, bool force_sw = false) = 0;

	// ��Ⱦһ֡.
	virtual bool render_one_frame(AVFrame* frame, int pix_fmt) = 0;

	// ������С.
	virtual void re_size(int width, int height){}

	// ���ÿ�߱�.
	virtual void aspect_ratio(int srcw, int srch, bool enable_aspect){}

	// ����render.
	virtual void destory_render(){}
	virtual bool use_overlay(){return false;}

    int get_width() {
		return mWidth;
	}

	int get_height() {
		return mHeight;
	}

    int get_swsMs() {
        return mAveScaleTimeMs;
    }

protected:
	void* mSurface;
	int mWidth;
	int mHeight;
	int mFormat;
	int mAveScaleTimeMs;
};

#endif // _RENDER_BASE_H_
