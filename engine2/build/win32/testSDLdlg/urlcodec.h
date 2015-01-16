#ifndef URL_CODEC_H
#define URL_CODEC_H

/**
* @param s ��Ҫ�����url�ַ���
* @param len ��Ҫ�����url�ĳ���
* @param new_length ������url�ĳ���
* @return char * ���ر�����url
* @note �洢������url�洢��һ����������ڴ��У�
* ����󣬵�����Ӧ���ͷ���
*/
char * urlencode(char const *s, int len, int *new_length);

/**
* @param str ��Ҫ�����url�ַ���
* @param len ��Ҫ�����url�ĳ���
* @return int ���ؽ�����url����
*/
int urldecode(char *str, int len);

#endif //URL_CODEC_H

