/**
 *Submitted for verification at DorChain on 2025-11-30
*/
// SPDX-License-Identifier: GPL-3.0
pragma solidity 0.8.28;

interface IERC20 {
    event Approval(address indexed owner, address indexed spender, uint value);
    event Transfer(address indexed from, address indexed to, uint value);

    function name() external view returns (string memory);
    function symbol() external view returns (string memory);
    function decimals() external view returns (uint8);
    function totalSupply() external view returns (uint);
    function balanceOf(address owner) external view returns (uint);
    function allowance(address owner, address spender) external view returns (uint);

    function approve(address spender, uint value) external returns (bool);
    function transfer(address to, uint value) external returns (bool);
    function transferFrom(address from, address to, uint value) external returns (bool);
    function autohl(uint256 amount)external;
}
interface Swap {
    function addLiquidity(
        address tokenA,
        address tokenB,
        uint amountADesired,
        uint amountBDesired,
        uint amountAMin,
        uint amountBMin,
        address to,
        uint deadline
    ) external returns (uint amountA, uint amountB, uint liquidity);
    function addLiquidityETH(
        address tokenA,
        uint amountTokenDesired,
        uint amountTokenMin,
        uint amountETHMin,
        address to,
        uint deadline
    ) external payable returns (uint amountToken, uint amountETH, uint liquidity);
    function removeLiquidityETHSupportingFeeOnTransferTokens(
        address tokenA,
        uint liquidity,
        uint amountTokenMin,
        uint amountETHMin,
        address to,
        uint deadline
    ) external returns (uint amountETH);
    function removeLiquidityETHWithPermitSupportingFeeOnTransferTokens(
        address tokenA,
        uint liquidity,
        uint amountTokenMin,
        uint amountETHMin,
        address to,
        uint deadline,
        bool approveMax, uint8 v, bytes32 r, bytes32 s
    ) external returns (uint amountETH);

    function swapExactTokensForTokensSupportingFeeOnTransferTokens(
        uint amountIn,
        uint amountOutMin,
        address[] calldata path,
        address to,
        uint deadline
    ) external;
    function swapExactETHForTokensSupportingFeeOnTransferTokens(
        uint amountOutMin,
        address[] calldata path,
        address to,
        uint deadline
    ) external payable;
    function swapExactTokensForETHSupportingFeeOnTransferTokens(
        uint amountIn,
        uint amountOutMin,
        address[] calldata path,
        address to,
        uint deadline
    ) external;
    function quote(uint amountA, uint reserveA, uint reserveB) external pure returns (uint amountB);
    function getAmountsOut(uint amountIn, address[] memory path) external view returns (uint[] memory amounts);
    function getAmountsIn(uint amountOut, address[] memory path)external view returns (uint[] memory amounts);
}
library TransferHelper {
    function safeApprove(address token, address to, uint value) internal {
        // bytes4(keccak256(bytes('approve(address,uint256)')));
        (bool success, bytes memory data) = token.call(abi.encodeWithSelector(0x095ea7b3, to, value));
        require(success && (data.length == 0 || abi.decode(data, (bool))), 'TransferHelper: APPROVE_FAILED');
    }

    function safeTransfer(address token, address to, uint value) internal {
        // bytes4(keccak256(bytes('transfer(address,uint256)')));
        (bool success, bytes memory data) = token.call(abi.encodeWithSelector(0xa9059cbb, to, value));
        require(success && (data.length == 0 || abi.decode(data, (bool))), 'TransferHelper: TRANSFER_FAILED');
    }

    function safeTransferFrom(address token, address from, address to, uint value) internal {
        // bytes4(keccak256(bytes('transferFrom(address,address,uint256)')));
        (bool success, bytes memory data) = token.call(abi.encodeWithSelector(0x23b872dd, from, to, value));
        require(success && (data.length == 0 || abi.decode(data, (bool))), 'TransferHelper: TRANSFER_FROM_FAILED');
    }

    function safeTransferETH(address to, uint value) internal {
        (bool success,) = to.call{value:value}(new bytes(0));
        require(success, 'TransferHelper: ETH_TRANSFER_FAILED');
    }
}
library SafeMath {
    function add(uint256 a, uint256 b) internal pure returns (uint256) {
        uint256 c = a + b;
        require(c >= a, "SafeMath: addition overflow");

        return c;
    }
    function sub(uint256 a, uint256 b) internal pure returns (uint256) {
        return sub(a, b, "SafeMath: subtraction overflow");
    }
    function sub(uint256 a, uint256 b, string memory errorMessage) internal pure returns (uint256) {
        require(b <= a, errorMessage);
        uint256 c = a - b;

        return c;
    }
    function mul(uint256 a, uint256 b) internal pure returns (uint256) {
        if (a == 0) {
            return 0;
        }

        uint256 c = a * b;
        require(c / a == b, "SafeMath: multiplication overflow");

        return c;
    }
    function div(uint256 a, uint256 b) internal pure returns (uint256) {
        return div(a, b, "SafeMath: division by zero");
    }
    function div(uint256 a, uint256 b, string memory errorMessage) internal pure returns (uint256) {
        // Solidity only automatically asserts when dividing by 0
        require(b > 0, errorMessage);
        uint256 c = a / b;
        // assert(a == b * c + a % b); // There is no case in which this doesn't hold


        return c;
    }
    function mod(uint256 a, uint256 b) internal pure returns (uint256) {
        return mod(a, b, "SafeMath: modulo by zero");
    }
    function mod(uint256 a, uint256 b, string memory errorMessage) internal pure returns (uint256) {
        require(b != 0, errorMessage);
        return a % b;
    }
}
interface lp {
    function balanceOf(address owner) external view returns (uint256);
    function addlp(uint256 _num,uint _t,uint256 _sn,address _user) external returns (bool);
    function removelp(uint256 _sn,address _acc)external returns (bool);
    function getlpusdt(uint256 _sn,address _acc)external returns (uint256);
    function getlpnum(uint256 _sn,address _acc)external returns (uint256);
}
contract Context {
    constructor ()  { }
    function _msgSender() internal view returns (address payable) {
        return payable(msg.sender);
    }
    function _msgData() internal view returns (bytes memory) {
        this; // silence state mutability warning without generating bytecode - see https://github.com/ethereum/solidity/issues/2691
        return msg.data;
    }
}
contract Ownable is Context {
    address private _owner;

    event OwnershipTransferred(address indexed previousOwner, address indexed newOwner);
    constructor ()  {
        address msgSender = _msgSender();
        _owner = msgSender;
        emit OwnershipTransferred(address(0), msgSender);
    }

    function owner() public view returns (address) {
        return _owner;
    }
    modifier onlyOwner() {
        require(_owner == _msgSender(), "Ownable: caller is not the owner");
        _;
    }
    function renounceOwnership() public onlyOwner {
        emit OwnershipTransferred(_owner, address(0));
        _owner = address(0);
    }
    function transferOwnership(address newOwner) public onlyOwner {
        _transferOwnership(newOwner);
    }
    function _transferOwnership(address newOwner) internal {
        require(newOwner != address(0), "Ownable: new owner is the zero address");
        emit OwnershipTransferred(_owner, newOwner);
        _owner = newOwner;
    }
}

contract TSEZQ is Context, Ownable {
    using SafeMath for uint256;
    address private _token=address(this);//合约地址
    address private _zeroacc=0x0000000000000000000000000000000000000000;

    address public mainrouter=address(0xd05088b5DF39E11ED0d63e78cCb8210572E2f9A7);
    address public factory=address(0xf3351c51E76d024a2e23C52bF7E8BaA98C7A5D87);

    address private DCNY=0x6c591df9001c0fBfa7935351E2500749Ecc66e26;
    address private TY=0x9F327d97dfb70d5D9ef2fc0c23e53D527C77765D;
    address private KYXFQDC=0x27D2ACda3384dF65C6D78DaA8cF9736855D435DA;//DCNY代持账号

    address public  TYLP=0xc899d9b42226f91D12bFf0d628A299C68ac8373f;//令牌合约
    address public  KYXFQHY=0xD19d51DE410B34075505FB5Ea60B414C5498fAAD;//可用消费券合约

    address public fhacc=0xccC6cfd50603F2B207b0626CB83073c4c2A64Cb4;//分红钱包
    address public yyacc=0x43C3F5cD40f3B21177d34A405f781700d7D2A0A4;//运营钱包
    address public ddacc=0xD5e2e465c42142651980Ff6232fAfDc603e3Baf8;//顶点账号
    address public initacc=0xc2B3c612Ac1F5B91Eb81D4a21f12bCfb5Ad57123;//推广账号
    address[] public fharr;

    mapping(address=>address) public pidarr;//推荐关系
    mapping(address=>uint256) private yjarr;//业绩
    mapping(address=>uint256) private myyjarr;//自身质押金额
    mapping(address=>uint256) public uhl;//用户回流金额
    mapping(address=>uint256) public ulasttime;//上一次操作时间

    uint256 public sn=10000;
    uint256 public maxusdt=20 * 10000 *1e18;
    uint256 public daytime=86400;
    uint256 public usdtdaytime=86400;
    uint256 public starttime=1764432000;//2025 - 11 -27
    mapping(uint => uint256) private dayusdt;
    mapping(uint => uint256) private jbarr;
    uint256 public mintse=1000*10000*1e18;

    uint256 public day45=10;
    uint256 public day90=15;
    uint256 public day180=20;
    uint256 public day360=30;
    uint256 public trate45=100;
    uint256 public trate90=100;
    uint256 public trate180=100;
    uint256 public trate360=100;
    uint256 public hlrate=20;//lp回流比例
    uint256 public tsehlrate=20;//TSE债券回流比例
    uint256 public k=1000;
    bool public TSESTAKEISOPEN=true;
    bool public KYXFQISOPEN=true;

    //债券
    struct bonds{
        uint256 num;//令牌数量
        uint256 starttime;//开始时间
        uint256 endtime;//结束时间
        uint256 v;//速度
        uint t;//类型
        bool status;//状态
        uint256 sn;//单号
        address acc;//用户
    }
    mapping(address => bonds[]) public  ubonds;
    //代币债券
    struct tbonds{
        uint256 num;//购买金额
        uint256 ylqbj;//已领取本金
        uint256 jj;//剩余奖金
        uint256 ylqjj;//已领取爆块奖励
        uint256 starttime;//开始时间
        uint256 lastday;//上次领取本金时间
        uint t;//类型
        uint rate;//复利利率
        bool status;//状态
        uint256 sn;//单号
        address acc;//用户
        uint256 buyje;//购买金额
    }
    struct restbonds{
        uint256 sn;//单号
        uint256 buyje;
        uint256 starttime;//开始时间
        uint256 klq;//可领取
        uint256 zzfs;//正在发送
        uint256 jj;//爆块奖励
        uint256 t;
    }
    mapping(address => tbonds[]) public  utbonds;

    constructor(){
        jbarr[1]=4;
        jbarr[2]=8;
        jbarr[3]=12;
        jbarr[4]=16;
        jbarr[5]=20;
    }

    function getOwner() external view returns (address) {
        return owner();
    }

    //购买债券
    function stake(uint256 amountusdt,uint _t)public returns(bool){
        require(pidarr[msg.sender]!=address(0),"NO PID");
        require(amountusdt <= 1000*1e18,"OUT MAX");
        require(block.timestamp.sub(ulasttime[msg.sender]) >= 30,"limit 30S");
        dayusdt[block.timestamp.sub(starttime)/usdtdaytime]=dayusdt[block.timestamp.sub(starttime)/usdtdaytime].add(amountusdt);
        require(dayusdt[block.timestamp.sub(starttime)/usdtdaytime]<=getdayusdt(),'NOT ALLOW USDT');
        if(_t!=45 && _t!=30 && _t!=1 && _t!=60){
            return false;
        }
        address _user=msg.sender;
        buyandadd(amountusdt,1);
        //发放令牌
        lp(TYLP).addlp(amountusdt,_t,sn,_user);
        sn=sn+1;
        //业绩统计
        myyjarr[_user]=myyjarr[_user].add(amountusdt);
        address pid = pidarr[_user];
        while(pid!=address(0)){
            yjarr[pid]=yjarr[pid].add(amountusdt);
            pid=pidarr[pid];
        }
        ulasttime[msg.sender]=block.timestamp;
        return true;
    }
    //可用消费券购买债券
    function dcny2stake(uint256 amountusdt,uint _t)public returns(bool){
        require(pidarr[msg.sender]!=address(0),"NO PID");
        require(KYXFQISOPEN==true,"NO OPEN");
        if(_t!=45 && _t!=30 && _t!=1 && _t!=60){
            return false;
        }
        require(amountusdt <= 1000*1e18,"OUT MAX");
        buyandadd(amountusdt,0);
        //发放令牌
        lp(TYLP).addlp(amountusdt,_t,sn,msg.sender);
        sn=sn+1;
        //业绩统计
        myyjarr[msg.sender]=myyjarr[msg.sender].add(amountusdt);
        address pid = pidarr[msg.sender];
        while(pid!=address(0)){
            yjarr[pid]=yjarr[pid].add(amountusdt);
            pid=pidarr[pid];
        }
        return true;
    }
    //赎回债券
    function unstake(uint256 _sn) public returns(bool){
        address _user = msg.sender;
        // 计算需要获得的DCNY金额
        uint256 amountOut = lp(TYLP).getlpusdt(_sn, _user);
        uint256 bj = lp(TYLP).getlpnum(_sn, _user);
        amountOut = amountOut.add(bj);

        uint256 bx=amountOut.mul(97).div(100);//本息
        //底池分红
        IERC20(DCNY).transfer(fhacc,bx.mul(12).div(1000));
        //运营
        IERC20(DCNY).transfer(yyacc,bx.mul(18).div(1000));
        uint256 profit=0;
        if(bx > bj){
            profit = bx.sub(bj); // 利润
        }
        //本金返回
        if(bx > bj){
            IERC20(DCNY).transfer(_user, bj);
        }else{
            IERC20(DCNY).transfer(_user, bx);
        }

        address[] memory path = new address[](2);
        path[0] = TY;
        path[1] = DCNY;

        // 使用getAmountsIn计算需要卖出的TY数量[3](@ref)
        uint[] memory amounts = Swap(mainrouter).getAmountsIn(amountOut, path);
        uint256 amountIn = amounts[0];

        // 执行兑换：卖出精确数量的TY，获得至少amountOut的DCNY[7](@ref)
        // uint256 amountOutmin=amountOut.mul(90).div(100);
        Swap(mainrouter).swapExactTokensForTokensSupportingFeeOnTransferTokens(
            amountIn,        // 修正：指定输入的TY数量
            0,       // 修正：期望得到的最小DCNY数量
            path,
            address(this),
            block.timestamp + 600
        );
        // 移除令牌
        lp(TYLP).removelp(_sn, _user);
        // 分配利润
        if(profit > 0){
            uint256 jj=profit.mul(70).div(100);
            lphl(_user,jj.mul(k).div(1000));

            doprofit(profit);
        }
        //业绩统计
        myyjarr[_user]=myyjarr[_user].sub(bj);
        address pid = pidarr[_user];
        while(pid!=address(0)){
            yjarr[pid]=yjarr[pid].sub(bj);
            pid=pidarr[pid];
        }
        if(IERC20(TY).balanceOf(address(this))<mintse){
            IERC20(TY).autohl(amountIn);
        }
        return true;
    }
//赎回债券并质押
    function unstakeandstake(uint256 _sn,uint256 _t) public returns(bool){
        if(_t!=45 && _t!=90 && _t!=180 && _t!=360){
            return false;
        }
        address _user = msg.sender;
        uint256 buyje=0;
        // 计算需要获得的DCNY金额
        uint256 amountOut = lp(TYLP).getlpusdt(_sn, _user);
        uint256 bj = lp(TYLP).getlpnum(_sn, _user);
        amountOut = amountOut.add(bj);

        uint256 bx=amountOut.mul(97).div(100);//本息
        //底池分红
        IERC20(DCNY).transfer(fhacc,bx.mul(12).div(1000));
        //运营
        IERC20(DCNY).transfer(yyacc,bx.mul(18).div(1000));
        uint256 profit=0;
        if(bx > bj){
            profit = bx.sub(bj); // 利润
        }
        //本金返回
        if(bx > bj){
            buyje=bj;
        }else{
            buyje=bx;
        }

        address[] memory path = new address[](2);
        path[0] = TY;
        path[1] = DCNY;

        // 使用getAmountsIn计算需要卖出的TY数量[3](@ref)
        uint[] memory amounts = Swap(mainrouter).getAmountsIn(amountOut, path);
        uint256 amountIn = amounts[0];

        // 执行兑换：卖出精确数量的TY，获得至少amountOut的DCNY[7](@ref)
        uint256 amountOutmin=amountOut.mul(90).div(100);
        Swap(mainrouter).swapExactTokensForTokensSupportingFeeOnTransferTokens(
            amountIn,        // 修正：指定输入的TY数量
            0,       // 修正：期望得到的最小DCNY数量
            path,
            address(this),
            block.timestamp + 600
        );

        // 移除令牌
        lp(TYLP).removelp(_sn, _user);

        // 分配利润
        if(profit > 0){
            uint256 jj=profit.mul(70).div(100);
            buyje = buyje.add(jj.mul(80).div(100));
            doprofit(profit);
        }
        //TSE质押
        tsestakeforunstake(buyje,_t);
        //更新用户业绩
        //业绩统计
        if(buyje>bj){
            uint256 amountusdt = buyje.sub(bj);
            myyjarr[msg.sender]=myyjarr[msg.sender].add(amountusdt);
            address pid = pidarr[msg.sender];
            while(pid!=address(0)){
                yjarr[pid]=yjarr[pid].add(amountusdt);
                pid=pidarr[pid];
            }
        }
        if(IERC20(TY).balanceOf(address(this))<mintse){
            IERC20(TY).autohl(amountIn);
        }
        return true;
    }
//赎回并质押
    function tsestakeforunstake(uint256 amountusdt,uint256 _t) private returns(bool){
        require(TSESTAKEISOPEN==true,"NO OPEN");
        if(_t!=45 && _t!=90 && _t!=180 && _t!=360){
            return false;
        }
        //获取代币价格
        address[] memory path = new address[](2);
        path[0]=TY;
        path[1]=DCNY;
        uint256 price=Swap(mainrouter).getAmountsOut(10**18,path)[1];
        uint256 zk=10;
        uint256 rate=1;
        if(_t==45){
            zk=day45;
            rate=trate45;
        }
        if(_t==90){
            zk=day90;
            rate=trate90;
        }
        if(_t==180){
            zk=day180;
            rate=trate180;
        }
        if(_t==360){
            zk=day360;
            rate=trate360;
        }
        buyandaddforunstake(amountusdt);
        //计算购买数量
        uint256 num=amountusdt.mul(1e18).div(price).mul(99+zk).div(100);
        //结构体写入
        utbonds[msg.sender].push(tbonds({
            num: num,
            ylqbj:0,
            jj:0,
            ylqjj:0,
            starttime: block.timestamp,
            lastday:0,
            t: _t,
            rate:rate,
            status: true,
            sn: sn,
            acc:msg.sender,
            buyje:amountusdt
        }));
        sn=sn+1;
        return true;
    }
//折价购买代币进行质押
    function tsestake(uint256 amountusdt,uint256 _t) public returns(bool){
        require(pidarr[msg.sender]!=address(0),"NO PID");
        require(TSESTAKEISOPEN==true,"NO OPEN");
        if(_t!=45 && _t!=90 && _t!=180 && _t!=360){
            return false;
        }
        //获取代币价格
        address[] memory path = new address[](2);
        path[0]=TY;
        path[1]=DCNY;
        uint256 price=Swap(mainrouter).getAmountsOut(10**18,path)[1];
        uint256 zk=10;
        uint256 rate=1;
        if(_t==45){
            zk=day45;
            rate=trate45;
        }
        if(_t==90){
            zk=day90;
            rate=trate90;
        }
        if(_t==180){
            zk=day180;
            rate=trate180;
        }
        if(_t==360){
            zk=day360;
            rate=trate360;
        }

        //  IERC20(DCNY).transferFrom(msg.sender, address(this), amountusdt);
        buyandadd(amountusdt,1);
        //计算购买数量
        uint256 num=amountusdt.mul(1e18).div(price).mul(99+zk).div(100);
        //结构体写入
        utbonds[msg.sender].push(tbonds({
            num: num,
            ylqbj:0,
            jj:0,
            ylqjj:0,
            starttime: block.timestamp,
            lastday:0,
            t: _t,
            rate:rate,
            status: true,
            sn: sn,
            acc:msg.sender,
            buyje:amountusdt
        }));
        sn=sn+1;
        //业绩统计
        myyjarr[msg.sender]=myyjarr[msg.sender].add(amountusdt);
        address pid = pidarr[msg.sender];
        while(pid!=address(0)){
            yjarr[pid]=yjarr[pid].add(amountusdt);
            pid=pidarr[pid];
        }
        return true;
    }

//本金领取
    function stakebj(uint _sn)public returns(bool){
        tbonds[] memory res = utbonds[msg.sender];
        uint256 canlq=0;
        uint256 nowjj=0;
        uint256 yjje=0;
        uint256 buyday=0;
        for(uint i=0;i<res.length;i++){
            uint256 day=block.timestamp.sub(res[i].starttime).div(daytime);
            if(res[i].sn==_sn){
                if(day > res[i].t){
                    day = res[i].t;
                }
                uint lastday = res[i].lastday;
                canlq=res[i].num.div(res[i].t).mul(day).sub(res[i].ylqbj);
                require(canlq > 0,'CANT GET CLAIM');
                //更新tbonds
                uint256 fljj = getfljj(res[i].starttime,res[i].lastday,res[i].rate,res[i].ylqbj,res[i].num,res[i].t);
                utbonds[msg.sender][i].ylqbj=utbonds[msg.sender][i].ylqbj.add(canlq);
                utbonds[msg.sender][i].lastday=day;
                utbonds[msg.sender][i].jj=utbonds[msg.sender][i].jj.add(fljj);
                buyday = day-lastday;
                yjje = res[i].buyje.mul(buyday).div(res[i].t);
            }
        }
        //发放TSE
        IERC20(TY).transfer(msg.sender, canlq.mul(97).div(100));
        //底池分红
        IERC20(TY).transfer(fhacc,canlq.mul(12).div(1000));
        //运营
        IERC20(TY).transfer(yyacc,canlq.mul(18).div(1000));
        //业绩统计
        myyjarr[msg.sender]=myyjarr[msg.sender].sub(yjje);
        address pid = pidarr[msg.sender];
        while(pid!=address(0)){
            yjarr[pid]=yjarr[pid].sub(yjje);
            pid=pidarr[pid];
        }
        return true;
    }
//爆块奖励领取
    function stakejj(uint _sn)public returns(bool){
        tbonds[] memory res = utbonds[msg.sender];
        uint256 canlq=0;
        uint256 nowjj=0;
        for(uint i=0;i<res.length;i++){
            uint256 day=block.timestamp.sub(res[i].starttime).div(daytime);
            if(res[i].sn==_sn){
                if(day > res[i].t){
                    day = res[i].t;
                }
                uint lastday = res[i].lastday;
                uint256 fljj = getfljj(res[i].starttime,res[i].lastday,res[i].rate,res[i].ylqbj,res[i].num,res[i].t);
                canlq=res[i].jj.add(fljj);
                require(canlq > 0,'CANT GET CLAIM');
                //更新tbonds
                utbonds[msg.sender][i].ylqjj=utbonds[msg.sender][i].ylqjj.add(canlq);
                utbonds[msg.sender][i].lastday=day;
                utbonds[msg.sender][i].jj=0;
            }
        }
        //发放TSE
        //给用户自己发放
        uint256 profit=canlq.mul(97).div(100);
        //底池分红
        IERC20(TY).transfer(fhacc,canlq.mul(12).div(1000));
        //运营
        IERC20(TY).transfer(yyacc,canlq.mul(18).div(1000));
        tsehl(msg.sender,profit.mul(70).div(100).mul(k).div(1000));
        doprofittse(profit);
        return true;
    }

    //利润分配--级差
    function doprofittse(uint256 profit) private returns (bool){
        profit = profit.mul(k).div(1000);
        uint256 jj=0;
        jj = profit.mul(5).div(100).mul(k).div(1000);
        tsehl(ddacc,jj);
        address pid=pidarr[msg.sender];
        //发放直推
        if(myyjarr[pid]>=2000*1e18){
            tsehl(pid,jj);
        }
        uint nowbc=0;
        while(pid!=address(0) && nowbc<20){
            uint256 yj = yjarr[pid];
            uint256 bl = 0;
            if(yj>=10*10000*1e18 && myyjarr[pid]>=2000*1e18){
                bl=4;
            }
            if(yj>=50*10000*1e18 && myyjarr[pid]>=2000*1e18){
                bl=8;
            }
            if(yj>=100*10000*1e18 && myyjarr[pid]>=2000*1e18){
                bl=12;
            }
            if(yj>=500*10000*1e18 && myyjarr[pid]>=2000*1e18){
                bl=16;
            }
            if(yj>=1000*10000*1e18 && myyjarr[pid]>=2000*1e18){
                bl=20;
            }
            if(bl>nowbc){
                uint ce = bl - nowbc;
                jj=profit.mul(ce).div(100);
                tsehl(pid,jj);
                nowbc = bl;
            }
            pid=pidarr[pid];
        }
        if(nowbc<20){
            jj=profit.mul(20-nowbc).div(100);
            tsehl(initacc,jj);
        }
        return true;
    }
    function tsehl(address _acc,uint256 jj)private returns(bool){
        IERC20(TY).transfer(_acc,jj.mul(100-tsehlrate).div(100));
        uint256 price=getprice();
        uhl[_acc]=uhl[_acc].add(jj.mul(tsehlrate).div(100).mul(price).div(1e18));
    }
    //利润分配--级差
    function doprofit(uint256 profit) private  returns (bool){
        uint256 jj=0;
        profit = profit.mul(k).div(1000);
        jj = profit.mul(5).div(100);
        lphl(ddacc,jj);
        address pid=pidarr[msg.sender];
        //发放直推
        if(myyjarr[pid]>=2000*1e18){
            lphl(pid,jj);
        }
        uint nowbc=0;
        while(pid!=address(0) && nowbc<20){
            uint256 yj = yjarr[pid];
            uint256 bl = 0;
            if(yj>=10*10000*1e18 && myyjarr[pid]>=2000*1e18){
                bl=4;
            }
            if(yj>=50*10000*1e18 && myyjarr[pid]>=2000*1e18){
                bl=8;
            }
            if(yj>=100*10000*1e18 && myyjarr[pid]>=2000*1e18){
                bl=12;
            }
            if(yj>=500*10000*1e18 && myyjarr[pid]>=2000*1e18){
                bl=16;
            }
            if(yj>=1000*10000*1e18 && myyjarr[pid]>=2000*1e18){
                bl=20;
            }
            if(bl>nowbc){
                uint ce = bl - nowbc;
                jj=profit.mul(ce).div(100);
                lphl(pid,jj);
                nowbc = bl;
            }
            pid=pidarr[pid];
        }
        if(nowbc<20){
            jj=profit.mul(20-nowbc).div(100);
            lphl(initacc,jj);
        }
        return true;
    }
    function lphl(address _acc,uint256 jj)private  returns(bool){
        IERC20(DCNY).transfer(_acc,jj.mul(100-hlrate).div(100));
        uhl[_acc]=uhl[_acc].add(jj.mul(hlrate).div(100));
    }

    //购买并加流动池
    function buyandadd(uint256 amount,uint flag) internal returns (bool) {
        // require(amount <=1000*1e18, "Amount must be greater than 0");

        // 1. Transfer DCNY tokens from user to this contract
        if(flag==1){
            IERC20(DCNY).transferFrom(msg.sender, address(this), amount);
        }else{
            IERC20(KYXFQHY).transferFrom(msg.sender, 0x000000000000000000000000000000000000dEaD, amount);
            IERC20(DCNY).transferFrom(KYXFQDC, address(this), amount);
        }

        address[] memory path = new address[](2);
        path[0] = DCNY;
        path[1] = TY;

        uint256 sellje = amount.mul(50).div(100);

        // 2. Record TY balance before swap
        uint256 tyBalanceBefore = IERC20(TY).balanceOf(address(this));

        // 3. Perform the swap
        Swap(mainrouter).swapExactTokensForTokensSupportingFeeOnTransferTokens(
            sellje,
            0, // 注意：设置最小输出数量为0有风险，建议设置合理最小值
            path,
            address(this), // 代币应发送到本合约地址以进行后续添加流动性操作
            block.timestamp + 600 // 使用相对时间戳，例如当前时间加10分钟
        );

        // 4. Calculate actual TY received
        uint256 tyBalanceAfter = IERC20(TY).balanceOf(address(this));
        uint256 tyReceived = tyBalanceAfter - tyBalanceBefore;
        require(tyReceived > 0, "No TY tokens received from swap");
        // 6. Add liquidity using the actual balances
        Swap(mainrouter).addLiquidity(
            DCNY,
            TY,
            sellje, // 实际打算使用的DCNY数量
            tyReceived, // 实际收到的TY数量
            0, // 设置合理的滑点容限（最小接受的DCNY数量）
            0, // 设置合理的滑点容限（最小接受的TY数量）
            address(0), // 流动性代币接收地址，这里设置为当前合约。根据你的需求，你也可以设置为msg.sender或其他地址。
            block.timestamp + 600
        );
        return true;
    }

//购买并加流动池
    function buyandaddforunstake(uint256 amount) private  returns (bool) {
        // require(amount <=1000*1e18, "Amount must be greater than 0");

        address[] memory path = new address[](2);
        path[0] = DCNY;
        path[1] = TY;

        uint256 sellje = amount.mul(50).div(100);

        // 2. Record TY balance before swap
        uint256 tyBalanceBefore = IERC20(TY).balanceOf(address(this));

        // 3. Perform the swap
        Swap(mainrouter).swapExactTokensForTokensSupportingFeeOnTransferTokens(
            sellje,
            1, // 注意：设置最小输出数量为0有风险，建议设置合理最小值
            path,
            address(this), // 代币应发送到本合约地址以进行后续添加流动性操作
            block.timestamp + 600 // 使用相对时间戳，例如当前时间加10分钟
        );

        // 4. Calculate actual TY received
        uint256 tyBalanceAfter = IERC20(TY).balanceOf(address(this));
        uint256 tyReceived = tyBalanceAfter - tyBalanceBefore;
        require(tyReceived > 0, "No TY tokens received from swap");
        // 6. Add liquidity using the actual balances
        Swap(mainrouter).addLiquidity(
            DCNY,
            TY,
            sellje, // 实际打算使用的DCNY数量
            tyReceived, // 实际收到的TY数量
            0, // 设置合理的滑点容限（最小接受的DCNY数量）
            0, // 设置合理的滑点容限（最小接受的TY数量）
            address(0), // 流动性代币接收地址，这里设置为当前合约。根据你的需求，你也可以设置为msg.sender或其他地址。
            block.timestamp + 600
        );
        return true;
    }

    //获取今天剩余额度
    function getdayusdt()public view returns(uint256){
        uint256 allowusdt=getmaxusdt();
        return allowusdt.sub(dayusdt[block.timestamp.sub(starttime)/usdtdaytime]);
    }

    //获取TY价格
    function getprice()public view returns(uint256){
        address[] memory path = new address[](2);
        path[0]=TY;
        path[1]=DCNY;
        uint256 price =  Swap(mainrouter).getAmountsOut(10**18,path)[1];
        return price;
    }
    //用户业绩查询
    function getyj(address acc)public view returns(uint256){
        return yjarr[acc];
    }
    function getallyj(address[] memory arr)public view returns(uint256[] memory){
        uint256[] memory res;
        res = new uint256[](arr.length);
        for(uint i=0;i<arr.length;i++){
            res[i]=yjarr[arr[i]];
        }
        return res;
    }
    function getmyyj(address acc)public view returns(uint256){
        return myyjarr[acc];
    }
    function getallmyyj(address[] memory arr)public view returns(uint256[] memory){
        uint256[] memory res;
        res = new uint256[](arr.length);
        for(uint i=0;i<arr.length;i++){
            res[i]=myyjarr[arr[i]];
        }
        return res;
    }

    //设置上级
    function setpid(address _acc)public returns(bool){
        require(_acc != address(0));// 上级不能为空地址
        require(_acc != msg.sender);// 上级不能为自己
        require(pidarr[msg.sender]==address(0));
        uint flag=0;
        address pid = _acc;
        while(flag==0 && pid!=address(0)){
            if(pid==msg.sender){
                flag=1;
            }
            pid = pidarr[pid];
        }
        require(flag==0,"You are the top");
        pidarr[msg.sender] = _acc;
        // ztarr[_acc].push(msg.sender);
        return true;         //返回成功标志
    }
    //获取上级
    function getpid(address _acc)public view returns(address){
        return pidarr[_acc];
    }
    function getallpid(address[] memory arr)public view returns(address[] memory){
        address[] memory res;
        res = new address[](arr.length);
        for(uint i=0;i<arr.length;i++){
            res[i]=pidarr[arr[i]];
        }
        return res;
    }
    //获取债券列表
    function gettbondslist(address _acc)public view returns(restbonds[] memory){
        tbonds[] memory list = utbonds[_acc];
        restbonds[] memory res = new restbonds[](list.length);
        for(uint i=0;i<list.length;i++){
            uint256 day=block.timestamp.sub(list[i].starttime).div(daytime);
            if(day > list[i].t){
                day=list[i].t;
            }
            uint256 klq=list[i].num.div(list[i].t).mul(day).sub(list[i].ylqbj);
            uint256 zzfs=list[i].num.sub(klq).sub(list[i].ylqbj);
            uint256 sn=list[i].sn;
            uint256 starttime=list[i].starttime;
            //奖金计算
            uint lastday = list[i].lastday;
            uint256 fljj = getfljj(list[i].starttime,list[i].lastday,list[i].rate,list[i].ylqbj,list[i].num,list[i].t);
            uint256 jj=list[i].jj.add(fljj);

            res[i].sn=sn;
            res[i].buyje=list[i].buyje;
            res[i].starttime=starttime;
            res[i].klq=klq;
            res[i].zzfs=zzfs;
            res[i].jj=jj;
            res[i].t=list[i].t;
        }
        return res;
    }

    function getfljj(uint256 starttime,uint256 lastday,uint256 rate,uint256 ylqbj,uint256 bj,uint256 t)public view returns(uint256){
        uint256 day=block.timestamp.sub(starttime).div(daytime);
        uint256 jj=0;
        if(day > t){
            day = t;
        }
        day = day - lastday;
        bj=bj.sub(ylqbj);
        uint256 bx=bj;
        for(uint i=0;i<day;i++){
            bx=bx.mul(10000+rate).div(10000);
        }
        jj=bx.sub(bj);
        return jj;
    }

    function approve()public returns(bool){
        IERC20(DCNY).approve(mainrouter,500000000000*1e18);
        IERC20(TY).approve(mainrouter,500000000000*1e18);
        return true;
    }
    function setk(uint256 _k) external onlyOwner {
        k = _k;
    }
    function setmaxusdt(uint256 _maxusdt) external onlyOwner {
        maxusdt = _maxusdt;
    }
    function drawall(address token)public onlyOwner{
        IERC20(token).transfer(msg.sender,IERC20(token).balanceOf(address(this)));
    }
    function settrate(uint256 _trate45,uint256 _trate90,uint256 _trate180,uint256 _trate360) public onlyOwner {
        trate45 =_trate45;
        trate90 =_trate90;
        trate180 =_trate180;
        trate360 =_trate360;
    }
    function setzk(uint256 _tday45,uint256 _tday90,uint256 _tday180,uint256 _tday360) public onlyOwner {
        day45 =_tday45;
        day90 =_tday90;
        day180 =_tday180;
        day360 =_tday360;
    }
    function sethl(uint256 _hlrate,uint256 _tsehlrate) public onlyOwner {
        hlrate =_hlrate;
        tsehlrate =_tsehlrate;
    }
    function setkyxfqisopen(bool flag) public onlyOwner {
        KYXFQISOPEN=flag;
    }
    function settseisopen(bool flag) public onlyOwner {
        TSESTAKEISOPEN=flag;
    }
    function getmaxusdt()public view returns(uint256) {
        uint256 day=block.timestamp.sub(starttime).div(usdtdaytime);
        if(day>30){
            day=30;
        }
        uint256 res = maxusdt;
        for(uint i=0;i<day;i++){
            res=res.div(10).add(res);
        }
        return res;
    }
    function getuhl(address acc)public view returns(uint256){
        return uhl[acc];
    }
    function getalluhl(address[] memory arr)public view returns(uint256[] memory){
        uint256[] memory res;
        res = new uint256[](arr.length);
        for(uint i=0;i<arr.length;i++){
            res[i]=uhl[arr[i]];
        }
        return res;
    }
    function addfharr(address acc) public onlyOwner returns (bool){
        fharr.push(acc);
        return true;
    }
    function setstarttime(uint256 _time) public onlyOwner returns (bool){
        starttime = _time;
        return true;
    }
    function getFharr() public view returns (address[] memory) {
        return fharr;
    }
    function setdaytime(uint256 _daytime,uint256 _usdtdaytime)public onlyOwner returns (bool){
        daytime=_daytime;
        usdtdaytime=_usdtdaytime;
    }
    function setlp(address _tylp)public onlyOwner returns (bool){
        TYLP=_tylp;
    }
}