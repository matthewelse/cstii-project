#include <pybind11/pybind11.h>
#include "matrix.h"

namespace py = pybind11;

int add(int i, int j) {
    return i + j;
}

PYBIND11_MODULE(simulation, m) {
    m.doc() = "pybind11 example plugin"; // optional module docstring

    m.def("add", &add, "A function which adds two numbers");

    py::class_<Matrix>(m, "Matrix", py::buffer_protocol())
       .def_buffer([](Matrix &m) -> py::buffer_info {
            return py::buffer_info(
                m.data(),                                   /* Pointer to buffer */
                sizeof(uint8_t),                            /* Size of one scalar */
                py::format_descriptor<uint8_t>::format(),   /* Python struct-style format descriptor */
                2,                                          /* Number of dimensions */
                { m.rows(), m.cols() },                     /* Buffer dimensions */
                { sizeof(uint8_t) * m.cols(),               /* Strides (in bytes) for each index */
                  sizeof(uint8_t) }
            );
        })
       .def(py::init([](py::buffer b) {
            /* Request a buffer descriptor from Python */
            py::buffer_info info = b.request();

            /* Some sanity checks ... */
            if (info.format != py::format_descriptor<uint8_t>::format())
                throw std::runtime_error("Incompatible format: expected a uint8_t array!");

            size_t w = info.shape[0];
            size_t h = info.shape[1];

            if (info.strides[0] != sizeof(uint8_t) * w || info.strides[1] != sizeof(uint8_t)) {
                throw std::runtime_error("Incompatible format: expected different stride.");
            }

            if (info.ndim != 2)
                throw std::runtime_error("Incompatible buffer dimension!");

            return new Matrix(reinterpret_cast<uint8_t*>(info.ptr), w, h);
        }))
       .def(py::init([](size_t w, size_t h) {
            return new Matrix(w, h);
        }))
       .def("set", &Matrix::set)
       .def("get", &Matrix::get)
       .def("__getitem__", [](Matrix &m, py::tuple t) {
            return m.get(t[0].cast<size_t>(), t[1].cast<size_t>());
       })
       .def("__setitem__", [](Matrix &m, py::tuple t, uint8_t value) {
            return m.set(t[0].cast<size_t>(), t[1].cast<size_t>(), value);
       });
}

