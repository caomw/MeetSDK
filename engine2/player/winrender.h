#ifndef _WIN_RENDER_H_
#define _WIN_RENDER_H_

#include "renderbase.h"

class WinRender : public RenderBase {
public:
	WinRender();
	~WinRender(void);

	// ��ʼrender.
	virtual bool init_render(void* ctx, int w, int h, int pix_fmt, bool force_sw = false);

	// ��Ⱦһ֡.
	virtual bool render_one_frame(AVFrame* frame, int pix_fmt);

private:
	void close();

	bool sws_sw(AVFrame *frame);
private:
	bool				mDoOnce;
	struct SwsContext*	mConvertCtx;
	AVFrame*			mSurfaceFrame;
};

#endif // _WIN_RENDER_H_