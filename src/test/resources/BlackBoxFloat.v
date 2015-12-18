module BBFZero(
    input  [0:0] clock,
    input  [0:0] reset,
    output [63:0] io$out
);
  assign io$out = $realtobits(0.0);
endmodule

module BBFOne(
    input  [0:0] clock,
    input  [0:0] reset,
    output [63:0] io$out
);
  assign io$out = $realtobits(1.0);
endmodule

module BBFTwo(
    input  [0:0] clock,
    input  [0:0] reset,
    output [63:0] io$out
);
  assign io$out = $realtobits(2.0);
endmodule

module BBFThree(
    input  [0:0] clock,
    input  [0:0] reset,
    output [63:0] io$out
);
  assign io$out = $realtobits(3.0);
endmodule

module BBFFour(
    input  [0:0] clock,
    input  [0:0] reset,
    output [63:0] io$out
);
  assign io$out = $realtobits(4.0);
endmodule

module BBFSix(
    input  [0:0] clock,
    input  [0:0] reset,
    output [63:0] io$out
);
  assign io$out = $realtobits(6.0);
endmodule

module BBFAdder(
    input  [0:0] clock,
    input  [0:0] reset,
    input  [63:0] io$in1,
    input  [63:0] io$in2,
    output [63:0] io$out
);
  real a = $bitstoreal(io$in1);
  real b = $bitstoreal(io$in2);
  real c = a + b;
  assign io$out = $realtobits(c);
endmodule

module BBFMult(
    input  [0:0] clock,
    input  [0:0] reset,
    input  [63:0] io$in1,
    input  [63:0] io$in2,
    output [63:0] io$out
);
  real a = $bitstoreal(io$in1);
  real b = $bitstoreal(io$in2);
  real c = a * b;
  assign io$out = $realtobits(c);
endmodule
