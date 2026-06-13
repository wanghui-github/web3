
import {IERC20} from "@openzeppelin/contracts/token/ERC20/IERC20.sol";

interface ILAXToken is IERC20 {
    function getYesterdayCloseReserveU() external view returns (uint112);
    function getCurrentReserveU() external view returns (uint112);
    function uniswapV2Pair() external view returns (address);
    function recycle(uint256 amount) external;
}