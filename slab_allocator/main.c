#include "allocator.h"
#include <stdio.h>
#include <stdlib.h>

int main() {
    void* arr[5];
    arr[0] = mem_alloc(20);
    arr[1] = mem_alloc(20);
    arr[2] = mem_alloc(20);
    arr[3] = mem_alloc(20);
    arr[4] = mem_alloc(30);
    void* virtual_page = mem_alloc(5000);
    mem_dump();
    mem_free(arr[0]);
    mem_free(arr[4]);
    mem_dump();
    virtual_page = mem_realloc(virtual_page, 10000);
    arr[1] = mem_realloc(arr[1], 40);
    mem_dump();
    mem_free(virtual_page);
    mem_free(arr[3]);
    mem_free(arr[2]);
    mem_free(arr[1]);
    return EXIT_SUCCESS;
}