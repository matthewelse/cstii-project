// See LICENSE for license details.
package conv

import chisel3._
import chisel3.util.{ValidIO, log2Ceil}

class SimpleConvolution extends Module {
  val io = IO(new Bundle {
    val pixel_in = Flipped(new ValidIO(SInt(8.W)))
    val pixel_out = new ValidIO(SInt(8.W))
    val set_weights = Input(Bool())
  })

  private val weight_width = 8.W
  private val weight_count = 3 * 3
  private val window_width = 16
  private val window_buffer = 2 * window_width + 3

  // circle buffer
  val weights = RegInit(
    VecInit(Seq.fill(weight_count) {
      0.S(weight_width)
    })
  )
  val weight_pos = RegInit(UInt(log2Ceil(weights.length).W), 0.U)

  val buffer = Mem(window_buffer, SInt(weight_width))
  val buffer_in = RegInit(0.U(log2Ceil(buffer.length).W))

  val out_valid = RegInit(false.B)

  def getPixel(mem: Mem[SInt], x: Int, y: Int, offset: UInt): SInt = {
    val position = offset + (y * window_width).U + x.U

    mem.read(position % buffer.length.U)
  }

  // this is our multiply/accumulate unit
  def convolve(mem: Mem[SInt], weights: Vec[SInt], pos: UInt): SInt  = {
    var total = 0.S

    for (i <- 0 to 2) {
      for (j <- 0 to 2) {
        val w = weights(j*3 + i)
        val p = getPixel(mem, i, j, pos)

        total = total + (w * p)
      }
    }

    total
  }

  io.pixel_out.valid := out_valid
  io.pixel_out.bits := convolve(buffer, weights, buffer_in)

  when (io.pixel_in.valid.toBool && io.set_weights.toBool) {
    // set the weight to the current input pixel
    weights(weight_pos) := io.pixel_in.bits

    when (weight_pos =/= (weights.length - 1).U) {
      weight_pos := weight_pos + 1.U
    }.otherwise {
      weight_pos := 0.U
    }
  }.elsewhen(io.pixel_in.valid.toBool) {
    // buffer the current input pixel
    buffer.write(buffer_in, io.pixel_in.bits)

    when (buffer_in =/= (buffer.length - 1).U) {
      buffer_in := buffer_in + 1.U
    }.otherwise {
      // if we're not outputting stuff yet, we should be now
      out_valid := true.B
      buffer_in := 0.U
    }
  }
}
