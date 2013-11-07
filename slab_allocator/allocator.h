#ifndef ALLOCATOR_H
#define ALLOCATOR_H

#include <stddef.h>

void* mem_alloc(size_t size);

void* mem_realloc(void* old_addr, size_t size);

void mem_copy(void* to, const void* from, const size_t bytes);

void mem_free(void* addr);

void mem_dump();

#endif

