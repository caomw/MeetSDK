// ���� ifdef ���Ǵ���ʹ�� DLL �������򵥵�
// ��ı�׼�������� DLL �е������ļ��������������϶���� LIBMEDIA_EXPORTS
// ���ű���ġ���ʹ�ô� DLL ��
// �κ�������Ŀ�ϲ�Ӧ����˷��š�������Դ�ļ��а������ļ����κ�������Ŀ���Ὣ
// LIBMEDIA_API ������Ϊ�Ǵ� DLL ����ģ����� DLL ���ô˺궨���
// ������Ϊ�Ǳ������ġ�
#ifdef LIBMEDIA_EXPORTS
#define LIBMEDIA_API __declspec(dllexport)
#else
#define LIBMEDIA_API __declspec(dllimport)
#endif

// �����Ǵ� libMedia.dll ������
class LIBMEDIA_API ClibMedia {
public:
	ClibMedia(void);
	// TODO: �ڴ�������ķ�����
};

extern LIBMEDIA_API int nlibMedia;

LIBMEDIA_API int fnlibMedia(void);
