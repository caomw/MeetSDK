
// testSDLdlg.h : PROJECT_NAME Ӧ�ó������ͷ�ļ�
//

#pragma once

#ifndef __AFXWIN_H__
	#error "�ڰ������ļ�֮ǰ������stdafx.h�������� PCH �ļ�"
#endif

#include "resource.h"		// ������


// CtestSDLdlgApp:
// �йش����ʵ�֣������ testSDLdlg.cpp
//

class CtestSDLdlgApp : public CWinApp
{
public:
	CtestSDLdlgApp();

// ��д
public:
	virtual BOOL InitInstance();

// ʵ��

	DECLARE_MESSAGE_MAP()
};

extern CtestSDLdlgApp theApp;