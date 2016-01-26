module BBFAdder(
    input  [0:0] clock,
    input  [0:0] reset,
    input  [63:0] io$in1,
    input  [63:0] io$in2,
    output reg [63:0] io$out
);
  always @* begin
  real a = $bitstoreal(io$in1);
  real b = $bitstoreal(io$in2);
  real c = a + b;
  io$out <= $realtobits(c);
  end
endmodule

module BBFMult(
    input  [0:0] clock,
    input  [0:0] reset,
    input  [63:0] io$in1,
    input  [63:0] io$in2,
    output [63:0] io$out
);
  always @* begin
  real a = $bitstoreal(io$in1);
  real b = $bitstoreal(io$in2);
  real c = a * b;
  io$out <= $realtobits(c);
  end
endmodule
