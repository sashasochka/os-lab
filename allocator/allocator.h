#ifndef ALLOCATOR_H
#define	ALLOCATOR_H

#include <stddef.h>
#include <stdbool.h>

/**
 * Allocate @size bytes of memory
 * @param size allocate @size bytes of memory
 * @return pointer to allocated memory
 */
void* mem_alloc(size_t size);

/**
 * Increase memory buffer pointed to by @addr to @new_size.
 * Memory could be moved - new memory address is returned
 * @param addr pointer to memory asked to be resized
 * @param new_size new size of the allocated memory buffer
 * @return new pointer to memory
 */
void* mem_realloc(void* addr, size_t new_size);

/**
 * Free memory pointed by @addr
 * @param addr address which points to data buffer which should be freed
 */
void mem_free(void* addr);

/**
 * Dump @size bytes of memory starting with byte pointed by @addr
 * @param addr Address of start of memory to be dumped
 * @param size Number of bytes to be dumped
 */
void mem_dump(const char* addr, const size_t size);


/**
 * Copy @bytes bytes from @from to @to
 * @param to
 * @param from
 * @param bytes
 */
void mem_copy(void* to, const void* from, const size_t bytes);

/**
 * Initialize allocator with buffer of size @size
 * @param size Number of bytes to allocate
 */
bool mem_init(const size_t size);

#endif	/* ALLOCATOR_H */
