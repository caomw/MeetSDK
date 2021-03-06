/*
 * Copyright (C) 2012 Roger Shen  rogershen@pptv.com
 *
 */


#ifndef PP_ERRORS_H
#define PP_ERRORS_H

#ifdef _MSC_VER
#include <stdint.h>
#else
#include <sys/types.h>
#endif

#include <errno.h>

#ifndef NULL
#define NULL 0
#endif

typedef int32_t status_t;
/*
 * Error codes. 
 * All error codes are negative values.
 */
 
enum {
    OK					= 0,    // Everything's swell.
	ERROR				= -1,    // errors.
    
    UNKNOWN_ERROR       = 0x80000000,

    NO_MEMORY           = -ENOMEM,
    INVALID_OPERATION   = -ENOSYS,
    BAD_VALUE           = -EINVAL,
    BAD_TYPE            = 0x80000001,
    NAME_NOT_FOUND      = -ENOENT,
    PERMISSION_DENIED   = -EPERM,
    NO_INIT             = -ENODEV,
    ALREADY_EXISTS      = -EEXIST,
    DEAD_OBJECT         = -EPIPE,
    FAILED_TRANSACTION  = 0x80000002,
    JPARKS_BROKE_IT     = -EPIPE,
#if !defined(HAVE_MS_C_RUNTIME)
    BAD_INDEX           = -EOVERFLOW,
    NOT_ENOUGH_DATA     = -ENODATA,
    WOULD_BLOCK         = -EWOULDBLOCK, 
    TIMED_OUT           = -ETIMEDOUT,
    UNKNOWN_TRANSACTION = -EBADMSG,
#else    
    BAD_INDEX           = -E2BIG,
    NOT_ENOUGH_DATA     = 0x80000003,
    WOULD_BLOCK         = 0x80000004,
    TIMED_OUT           = 0x80000005,
    UNKNOWN_TRANSACTION = 0x80000006,
#endif


	READ_OK				= 0x10000000,
	READ_REACH_STREAM_EDN,
	READ_ABORTED,
	READ_PACKET_UNAVAILABLE,

	// 2015.4.1 guoliangma added
	READ_EOF			= 0x20000000,
};

#endif // PP_ERRORS_H
