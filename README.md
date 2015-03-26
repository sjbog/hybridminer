## Hybrid Processes Miner

Reconstructs hybrid process models from logs.

To build a jar, include to project dependencies:
 
 1) ./lib
 
 2) ProM/ProM___.jar
 
 3) ProM/lib


### Algorithm limitations:
 loops
 unique event names (duplicate activities aren't allowed)
 log completeness (all edge cases are present in the log at least once)
 ambiguously assigned activities to an incorrect branch
 noise-free
 event transition isn't accessible in ProcessTree (only works with complete events) 



### License
Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
