# web3
java repo with web3

solcjs --bin --abi src/main/resources/contracts/SimpleStorage.sol -o build/

web3j generate solidity -a build/SimpleStorage.abi -b build/SimpleStorage.bin -o src/main/java -p web3.contract.generated
