# Firejail profile for java
# Persistent local customizations
#include java.local
# Persistent global definitions
#include globals.local

include allow-java.inc

### basic blacklisting
include disable-common.inc
# include disable-devel.inc
# include disable-exec.inc
# include disable-interpreters.inc
include disable-passwdmgr.inc
# include disable-programs.inc
# include disable-xdg.inc

### home directory whitelisting
whitelist ${HOME}/kuromasu/work
include whitelist-common.inc

### filesystem
# /usr/share:
blacklist /usr/share
# /var:
blacklist /var

# $PATH:
# /dev:
private-dev
# /etc:
private-etc passwd,
# /tmp:

# private-tmp
# File accessed in /tmp directory:
# /tmp/hsperfdata_debian,

### security filters
caps.drop all
nonewprivs
seccomp
# seccomp.keep futex,gettid,write,mprotect,mmap,getrusage,sysinfo,madvise,lseek,munmap,clone,pread64,read,openat,rt_sigprocmask,sched_getaffinity,prctl,set_robust_list,stat,lstat,fstat,close,sched_yield,rt_sigaction,clock_nanosleep,fcntl,getcwd,getpid,dup,prlimit64,brk,rt_sigreturn,ioctl,access,socket,connect,execve,uname,ftruncate,fchdir,mkdir,unlink,readlink,getuid,geteuid,arch_prctl,getdents64,set_tid_address,clock_getres
# 49 syscalls total
# Probably you will need to add more syscalls to seccomp.keep. Look for
# seccomp errors in /var/log/syslog or /var/log/audit/audit.log while
# running your sandbox.

### network
net none

### environment
shell none

