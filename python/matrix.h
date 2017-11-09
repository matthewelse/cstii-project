// Copyright 2017 Matthew Else
#pragma once

class Matrix {
 public:
    /**
     * Create a uint8_t matrix with a specified width and height.
     */
    Matrix(size_t width, size_t height) : width(width), height(height) {
        _data = new uint8_t[width * height];
    }

    Matrix(uint8_t *_from, size_t width, size_t height) : width(width), height(height) {
        _data = _from;
    }

    /**
     * Free all memory used by the matrix object. This should only be the underlying data buffer.
     */
    ~Matrix() {
        delete _data;
    }

    /**
     * Reads the value at x, y
     */
    uint8_t get(size_t x, size_t y) {
        if (x >= width || y >= height) {
            throw std::out_of_range("x, y out of range of the array");
        }

        return _data[(y*width) + x];
    }

    /**
     * Sets the value at x, y
     */
    void set(size_t x, size_t y, uint8_t new_value) {
        if (x >= width || y >= height) {
            throw std::out_of_range("x, y out of range of the array");
        }

        _data[(y*width) + x] = new_value;
    }

    uint8_t &operator() (size_t x, size_t y) {
        if (x >= width || y >= height) {
            throw std::out_of_range("x, y out of range of the array");
        }

        return _data[(y*width) + x];
    }

    uint8_t &operator[] (size_t pos) {
        if (pos >= (width * height)) {
            throw std::out_of_range("pos outside the bounds of the matrix");
        }

        return _data[pos];
    }

    size_t rows() {
        return height;
    }

    size_t cols() {
        return width;
    }

    uint8_t *data() {
        return _data;
    }

 private:
    uint8_t *_data;

    size_t width;
    size_t height;
};
