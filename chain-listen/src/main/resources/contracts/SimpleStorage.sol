/**
 *Submitted for verification at DorChain on 2026-1-9
*/
// SPDX-License-Identifier: GPL-3.0
pragma solidity 0.8.28;
interface IERC20 {
    event Approval(address indexed owner, address indexed spender, uint value);
    event Transfer(address indexed from, address indexed to, uint value);
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
    function swapExactTokensForTokensSupportingFeeOnTransferTokens(
        uint amountIn,
        uint amountOutMin,
        address[] calldata path,
        address to,
        uint deadline
    ) external;
    function getAmountsOut(uint amountIn, address[] memory path) external view returns (uint[] memory amounts);
    function getAmountsIn(uint amountOut, address[] memory path)external view returns (uint[] memory amounts);
}
interface TYZQ{
    function getpid(address _acc)external view returns(address);
    function getyj(address acc)external view returns(uint256);
    function getmyyj(address acc)external view returns(uint256);
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
        require(b > 0, errorMessage);
        uint256 c = a / b;
        return c;
    }
}
contract Context {
    constructor ()  { }
    function _msgSender() internal view returns (address payable) {
        return payable(msg.sender);
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
}
contract TSEZQ is Context, Ownable {
    using SafeMath for uint256;
    address private _token=address(this);//合约地址
    address private _zeroacc=0x0000000000000000000000000000000000000000;
    address public mainrouter=address(0xd05088b5DF39E11ED0d63e78cCb8210572E2f9A7);
    address public factory=address(0xf3351c51E76d024a2e23C52bF7E8BaA98C7A5D87);
    address private DCNY=0x6dCef99b8be7933355223a57c31c857d70261fAB;//测试
    address private TY=0xDBE6E6e6Dd0f56869B852A229f38BeA5850C31E8;//测试
    address public  KYXFQHY=0x54AF17aCAB86Dd72373eD4EbC5f1B7d43e44FF1e;//可用消费券合约 测试
    address private KYXFQDC=0x27D2ACda3384dF65C6D78DaA8cF9736855D435DA;//DCNY代持账号
    address public fhacc=0xccC6cfd50603F2B207b0626CB83073c4c2A64Cb4;//分红钱包
    address public yyacc=0x43C3F5cD40f3B21177d34A405f781700d7D2A0A4;//运营钱包
    address public ddacc=0xD5e2e465c42142651980Ff6232fAfDc603e3Baf8;//顶点账号
    address public initacc=0xc2B3c612Ac1F5B91Eb81D4a21f12bCfb5Ad57123;//推广账号
    address[] public fharr;

    mapping(address=>address) public pidarr;//推荐关系
    mapping(address=>uint256) private yjarr;//业绩
    mapping(address=>uint256) private myyjarr;//自身质押金额
    mapping(address=>uint256) private my180yjarr;//自身质押金额
    mapping(address=>uint256) private my365yjarr;//自身质押金额
    mapping(address=>uint256) public uhl;//用户回流金额
    mapping(address=>uint256) public ulasttime;//上一次操作时间
    uint256 public sn=10000;
    uint256 public maxdayky=371 * 10000 *1e18;//KY天限额
    uint256 public maxdaydcny=371 * 10000 *1e18;//DCNY天限额
    uint256 public daytime=86400;
    uint256 public starttime=1764432000;
    mapping(uint => uint256) private dayky;
    mapping(uint => uint256) private daydcny;
    mapping(uint => uint256) private lpdcnyrate;//DCNY质押利率
    mapping(uint => uint256) private lpkyrate;//KY质押利率
    uint256 public mintse=10*10000*1e18;
    uint256 public hlrate=30;//lp回流比例
    uint256 public tsehlrate=30;//TSE债券回流比例
    uint256 public k=1000;
    uint256 public tseday=90;
    uint256 public tserate=15;
    bool public istse=false;
    struct lp{
        uint256 num;//本金
        uint256 starttime;//开始时间
        uint256 endtime;//结束时间
        uint256 rate;//利率
        uint t;//类型 周期
        bool status;//状态
        uint256 sn;//单号
        address acc;//用户
        uint256 jj;//利息
        uint256 isky;//0 DCNY 1 KY
    }
    mapping(address => lp[]) public  ulp;
    struct lp180{
        uint256 num;//本金
        uint256 starttime;//开始时间
        uint256 endtime;//结束时间
        bool status;//状态
        uint256 sn;//单号
        address acc;//用户
        uint256 jj;//利息
    }
    mapping(address => lp180[]) public  ulp180;
    struct lptse{
        uint256 num;//tse
        uint256 bj;//本金
        uint256 starttime;//开始时间
        uint256 endtime;//结束时间
        bool status;//状态
        uint256 sn;//单号
        address acc;//用户
        uint256 jj;//利息
        uint256 rate;//利率
        uint256 tseday;//周期
    }
    mapping(address => lptse[]) public  ulptse;
    struct lp365{
        uint256 num;//本金
        uint256 starttime;//开始时间
        uint256 endtime;//结束时间
        bool status;//状态
        uint256 sn;//单号
        address acc;//用户
        uint256 jj;//利息
        uint256 lastday;//上次领取的天数
        uint256 ylqjj;//已领取利息
    }
    mapping(address => lp365[]) public  ulp365;
    constructor(){
        lpdcnyrate[30]=100;
        lpkyrate[30]=50;
    }
    function dcnystake(uint256 _amount,uint _t,uint256 isky)public returns(bool){
        require(pidarr[msg.sender]!=address(0),"NO PID");
        require(_amount <= 1000*1e18,"OUT MAX");
        require(block.timestamp.sub(ulasttime[msg.sender]) >= 30,"limit 30S");
        if(isky==0){
            daydcny[block.timestamp.sub(starttime)/daytime]=daydcny[block.timestamp.sub(starttime)/daytime].add(_amount);
            require(daydcny[block.timestamp.sub(starttime)/daytime]<=maxdaydcny,'NOT ALLOW DCNY');
        }else{
            dayky[block.timestamp.sub(starttime)/daytime]=dayky[block.timestamp.sub(starttime)/daytime].add(_amount);
            require(dayky[block.timestamp.sub(starttime)/daytime]<=maxdayky,'NOT ALLOW KY');
        }
        address _user=msg.sender;
        if(isky==0){
            buyandadd(_amount,1);
        }else{
            buyandadd(_amount,0);
        }
        uint256 _starttime = block.timestamp;
        uint256 _endtime = _starttime.add(_t*daytime);
        uint256 _rate = 0;
        if(isky==0){
            _rate = lpdcnyrate[_t];
        }else{
            _rate = lpkyrate[_t];
        }
        require(_rate > 0,"NO RATE");
        ulp[_user].push(lp({
            num: _amount,
            starttime: _starttime,
            endtime: _endtime,
            rate: _rate,
            t: _t,
            status: true,
            sn:sn,
            acc:_user,
            jj:0,
            isky:isky
        }));
        sn=sn+1;
        myyjarr[_user]=myyjarr[_user].add(_amount);
        address pid = pidarr[_user];
        while(pid!=address(0)){
            yjarr[pid]=yjarr[pid].add(_amount);
            pid=pidarr[pid];
        }
        ulasttime[msg.sender]=block.timestamp;
        return true;
    }
    //赎回LP债券
    function kydcnyunstake(uint256 _sn)private returns(uint256){
        address _user = msg.sender;
        uint256 bj=0;
        uint256 endtime=block.timestamp.add(daytime);
        bool status=false;
        // 计算需要获得的DCNY金额
        uint256 amountOut = getlpusdt(_sn, _user);
        lp[] storage userLps = ulp[_user];
        for (uint i = 0; i < userLps.length; i++){
            if(userLps[i].sn == _sn && userLps[i].acc == _user) {
                endtime = userLps[i].endtime;
                status = userLps[i].status;
                bj=userLps[i].num;
                ulp[_user][i].status=false;
            }
        }
        require(block.timestamp > endtime,"THE TIME IS ERROR");
        require(status==true,"THE STATUS IS FALSE");
        amountOut = amountOut.add(bj);

        uint256 bx=amountOut.mul(97).div(100);//本息
        IERC20(DCNY).transfer(fhacc,bx.mul(12).div(1000));
        IERC20(DCNY).transfer(yyacc,bx.mul(18).div(1000));
        uint256 profit=0;
        require(bx>bj,"THE BX IS MIN");
        profit = bx.sub(bj); // 利润
        IERC20(DCNY).transfer(_user,bj);

        address[] memory path = new address[](2);
        path[0] = TY;
        path[1] = DCNY;
        uint[] memory amounts = Swap(mainrouter).getAmountsIn(amountOut, path);
        uint256 amountIn = amounts[0];
        Swap(mainrouter).swapExactTokensForTokensSupportingFeeOnTransferTokens(
            amountIn,
            0,
            path,
            address(this),
            block.timestamp + 600
        );
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
        return bj;
    }
    function lp30tse(uint256 _sn)public returns(bool){
        uint256 amountdcny = kydcnyunstake(_sn);
        if(istse==true){
            tse90stake(amountdcny);
        }
        return true;
    }
    function dcny180stake(uint256 amountdcny)public returns(bool){
        address _user=msg.sender;
        buyandadd(amountdcny,1);
        uint256 _starttime = block.timestamp;
        uint256 _endtime = _starttime.add(180*daytime);
        ulp180[_user].push(lp180({
            num: amountdcny,
            starttime: _starttime,
            endtime: _endtime,
            status: true,
            sn:sn,
            acc:_user,
            jj:0
        }));
        sn=sn+1;
        //业绩统计
        myyjarr[_user]=myyjarr[_user].add(amountdcny);
        my180yjarr[_user]=my180yjarr[_user].add(amountdcny);
        address pid = pidarr[_user];
        while(pid!=address(0)){
            yjarr[pid]=yjarr[pid].add(amountdcny);
            pid=pidarr[pid];
        }
        return true;
    }
    //赎回L180债券
    function unstakelp180(uint256 _sn) private  returns(uint256){
        address _user = msg.sender;
        // 计算需要获得的DCNY金额
        //修改状态
        uint256 amountOut;
        uint256 bj;
        uint256 endtime=block.timestamp.add(daytime);
        bool status=false;
        lp180[] storage userLps = ulp180[msg.sender];
        for (uint i = 0; i < userLps.length; i++){
            if(userLps[i].sn == _sn && userLps[i].acc == msg.sender) {
                status=userLps[i].status;
                endtime=userLps[i].endtime;
                ulp180[_user][i].status = false;
                bj = userLps[i].num;
                amountOut = userLps[i].num.mul(30).div(100);
            }
        }
        require(block.timestamp > endtime,"THE TIME IS ERROR");
        require(status==true,"THE STATUS IS FALSE");
        amountOut = amountOut.add(bj);
        uint256 bx=amountOut.mul(97).div(100);//本息
        //底池分红
        IERC20(DCNY).transfer(fhacc,bx.mul(12).div(1000));
        //运营
        IERC20(DCNY).transfer(yyacc,bx.mul(18).div(1000));
        uint256 profit=0;
        profit = bx.sub(bj); // 利润
        IERC20(DCNY).transfer(_user, bj);
        address[] memory path = new address[](2);
        path[0] = TY;
        path[1] = DCNY;
        uint[] memory amounts = Swap(mainrouter).getAmountsIn(amountOut, path);
        uint256 amountIn = amounts[0];
        Swap(mainrouter).swapExactTokensForTokensSupportingFeeOnTransferTokens(
            amountIn,        // 修正：指定输入的TY数量
            0,       // 修正：期望得到的最小DCNY数量
            path,
            address(this),
            block.timestamp + 600
        );
        // 分配利润
        uint256 jj=profit.mul(70).div(100);
        lphl(_user,jj.mul(k).div(1000));
        doprofit(profit);
        //业绩统计
        my180yjarr[_user]=my180yjarr[_user].sub(bj);
        address pid = pidarr[_user];
        while(pid!=address(0)){
            yjarr[pid]=yjarr[pid].sub(bj);
            pid=pidarr[pid];
        }
        if(IERC20(TY).balanceOf(address(this))<mintse){
            IERC20(TY).autohl(amountIn);
        }
        return bj;
    }
    function lp180tse(uint256 _sn)public returns(bool){
        uint256 amountdcny = unstakelp180(_sn);
        if(istse==true){
            tse90stake(amountdcny);
        }
        return true;
    }
    function dcnyky365stake(uint256 amountdcny)public returns(bool){
        address _user=msg.sender;
        IERC20(KYXFQHY).transferFrom(msg.sender, 0x000000000000000000000000000000000000dEaD,amountdcny);
        buyandadd(amountdcny,1);
        //记录
        uint256 _starttime = block.timestamp;
        uint256 _endtime = _starttime.add(365*daytime);
        ulp365[_user].push(lp365({
            num: amountdcny,
            starttime: _starttime,
            endtime: _endtime,
            status: true,
            sn:sn,
            acc:_user,
            jj:0,
            lastday:0,
            ylqjj:0
        }));
        sn=sn+1;
        //业绩统计
        myyjarr[_user]=myyjarr[_user].add(amountdcny.mul(2));
        my365yjarr[_user]=my365yjarr[_user].add(amountdcny.mul(2));
        return true;
    }
    function dcnyky365jj(uint _sn)public returns(bool){
        lp365[] memory res = ulp365[msg.sender];
        address _user = msg.sender;
        uint256 canlq=0;
        uint256 nowjj=0;
        for(uint i=0;i<res.length;i++){
            uint256 day=block.timestamp.sub(res[i].starttime).div(daytime);
            if(res[i].sn==_sn){
                if(day > 365){
                    day = 365;
                }
                uint lastday = res[i].lastday;
                uint256 fljj = res[i].num.mul(day.sub(lastday)).mul(5).div(1000);
                canlq=fljj;
                require(canlq > 0,'CANT GET CLAIM');
                //更新ulp365
                ulp365[msg.sender][i].ylqjj=ulp365[msg.sender][i].ylqjj.add(canlq);
                ulp365[msg.sender][i].lastday=day;
                ulp365[msg.sender][i].jj=0;
            }
        }
        uint256 jj=canlq.mul(70).div(100);
        lphl(_user,jj.mul(k).div(1000));
        doprofit(canlq);
        return true;
    }
    function dcnyky365bj(uint _sn)private returns(uint256){
        lp365[] memory res = ulp365[msg.sender];
        address _user = msg.sender;
        uint256 bj=0;
        uint256 endtime=block.timestamp.add(daytime);
        bool status=false;
        for(uint i=0;i<res.length;i++){
            if(res[i].sn==_sn && ulp365[msg.sender][i].status==true){
                require(block.timestamp > ulp365[msg.sender][i].endtime,"THE TIME IS ERROR");
                ulp365[msg.sender][i].status=false;
                bj=ulp365[msg.sender][i].num;
                IERC20(DCNY).transfer(msg.sender,bj.mul(97).div(100));
            }
        }
        myyjarr[_user]=myyjarr[_user].add(bj.mul(2));
        my365yjarr[_user]=my365yjarr[_user].add(bj.mul(2));
        return bj;
    }
    function lp365tse(uint256 _sn)public returns(bool){
        uint256 amountdcny = dcnyky365bj(_sn);
        if(istse==true){
            tse90stake(amountdcny);
        }
        return true;
    }
    function tse90stake(uint256 amountdcny)public returns(bool){
        address _user=msg.sender;
        address[] memory path = new address[](2);
        path[0]=TY;
        path[1]=DCNY;
        uint256 price=Swap(mainrouter).getAmountsOut(10**18,path)[1];
        buyandadd(amountdcny,1);
        uint256 num=amountdcny.mul(1e18).div(price).mul(100).div(100);
        //记录
        uint256 _starttime = block.timestamp;
        uint256 _endtime = _starttime.add(tseday*daytime);
        ulptse[_user].push(lptse({
            num: num,
            bj:amountdcny,
            starttime: _starttime,
            endtime: _endtime,
            status: true,
            sn:sn,
            acc:_user,
            jj:0,
            rate:tserate,
            tseday:tseday
        }));
        sn=sn+1;
        //业绩统计
        myyjarr[_user]=myyjarr[_user].add(amountdcny);
        address pid = pidarr[_user];
        while(pid!=address(0)){
            yjarr[pid]=yjarr[pid].add(amountdcny);
            pid=pidarr[pid];
        }
        return true;
    }
    function unstaketse90(uint256 _sn) public returns(bool){
        address _user = msg.sender;
        //修改状态
        uint256 amountOut=0;
        uint256 bj=0;
        uint256 dcnybj=0;
        uint256 tseday=0;
        uint256 endtime=block.timestamp.add(daytime);
        bool status=false;
        lptse[] storage userLps = ulptse[msg.sender];
        for (uint i = 0; i < userLps.length; i++){
            if(userLps[i].sn == _sn && userLps[i].acc == msg.sender) {
                require(block.timestamp>userLps[i].endtime,"THE TIME IS ERROR");
                require(userLps[i].status==true);
                userLps[i].status = false;
                bj = userLps[i].num;
                tseday = userLps[i].tseday;
                dcnybj = userLps[i].bj;
                amountOut = userLps[i].num.mul(userLps[i].rate).div(100);
                return true;
            }
        }
        amountOut = amountOut.add(bj);
        uint256 bx=amountOut.mul(97).div(100);//本息
        //底池分红
        IERC20(TY).transfer(fhacc,bx.mul(12).div(1000));
        //运营
        IERC20(TY).transfer(yyacc,bx.mul(18).div(1000));
        uint256 profit=0;
        if(bx > bj){
            profit = bx.sub(bj); // 利润
        }
        //本金返回
        IERC20(TY).transfer(_user, bj);
        // 分配利润
        uint256 jj=profit.mul(70).div(100);
        lphl(_user,jj.mul(k).div(1000));
        doprofittse(profit);
        //业绩统计
        myyjarr[_user]=myyjarr[_user].sub(dcnybj);
        address pid = pidarr[_user];
        while(pid!=address(0)){
            yjarr[pid]=yjarr[pid].sub(dcnybj);
            pid=pidarr[pid];
        }
        return true;
    }
    function getlp30list(address _acc)public view returns(lp[] memory){
        lp[] memory userLps = ulp[_acc];
        for(uint i=0;i<userLps.length;i++){
            uint256 starttime=userLps[i].starttime;
            uint256 t=userLps[i].t;
            uint256 rate=userLps[i].rate;
            uint256 bj=userLps[i].num;
            uint256 endtime=userLps[i].endtime;
            bool status=userLps[i].status;
            uint256 jj=getlpfljj(starttime, t, rate, bj,status,endtime);
            userLps[i].jj=jj;
        }
        return userLps;
    }
    function getlp180list(address _acc)public view returns(lp180[] memory){
        lp180[] memory userLps = ulp180[_acc];
        for(uint i=0;i<userLps.length;i++){
            uint256 starttime=userLps[i].starttime;
            uint256 bj=userLps[i].num;
            uint256 endtime=userLps[i].endtime;
            bool status=userLps[i].status;
            uint256 day=block.timestamp.sub(starttime).div(daytime);
            if(day>180){
                day=180;
            }
            uint256 jj=bj.mul(day).mul(30).div(100).div(180);
            userLps[i].jj=jj;
        }
        return userLps;
    }
    function gettselist(address _acc)public view returns(lptse[] memory){
        lptse[] memory userLps = ulptse[_acc];
        for(uint i=0;i<userLps.length;i++){
            uint256 starttime=userLps[i].starttime;
            uint256 bj=userLps[i].num;
            uint256 endtime=userLps[i].endtime;
            bool status=userLps[i].status;
            uint256 day=block.timestamp.sub(starttime).div(daytime);
            if(day>userLps[i].tseday){
                day=userLps[i].tseday;
            }
            uint256 jj=bj.mul(day).mul(userLps[i].rate).div(userLps[i].tseday);
            userLps[i].jj=jj;
        }
        return userLps;
    }
    function getlp365list(address _acc)public view returns(lp365[] memory){
        lp365[] memory userLps = ulp365[_acc];
        for(uint i=0;i<userLps.length;i++){
            uint256 starttime=userLps[i].starttime;
            uint256 bj=userLps[i].num;
            uint256 endtime=userLps[i].endtime;
            bool status=userLps[i].status;
            uint256 lastday=block.timestamp.sub(starttime).div(daytime);
            if(lastday>365){
                lastday=365;
            }
            uint256 day= lastday.sub(userLps[i].lastday);
            uint256 jj=bj.mul(day).mul(5).div(1000);
            userLps[i].jj=jj;
        }
        return userLps;
    }
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
    //购买并加流动池
    function buyandadd(uint256 amount,uint flag) internal returns (bool) {
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
        uint256 tyBalanceBefore = IERC20(TY).balanceOf(address(this));
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
        uint256 oldyj=TYZQ(0x40f5543eF2a9cBE429b26dED8e50298e7A67f12d).getyj(acc);
        return yjarr[acc].add(oldyj);
    }
    function getallyj(address[] memory arr)public view returns(uint256[] memory){
        uint256[] memory res;
        res = new uint256[](arr.length);
        for(uint i=0;i<arr.length;i++){
            uint256 oldyj=TYZQ(0x40f5543eF2a9cBE429b26dED8e50298e7A67f12d).getyj(arr[i]);
            res[i]=yjarr[arr[i]].add(oldyj);
        }
        return res;
    }
    function getmyyj(address acc)public view returns(uint256){
        uint256 oldyj=TYZQ(0x40f5543eF2a9cBE429b26dED8e50298e7A67f12d).getmyyj(acc);
        return myyjarr[acc].add(oldyj);
    }
    function getallmyyj(address[] memory arr)public view returns(uint256[] memory){
        uint256[] memory res;
        res = new uint256[](arr.length);
        for(uint i=0;i<arr.length;i++){
            uint256 oldyj=TYZQ(0x40f5543eF2a9cBE429b26dED8e50298e7A67f12d).getmyyj(arr[i]);
            res[i]=myyjarr[arr[i]].add(oldyj);
        }
        return res;
    }
    function getall180yj(address[] memory arr)public view returns(uint256[] memory){
        uint256[] memory res;
        res = new uint256[](arr.length);
        for(uint i=0;i<arr.length;i++){
            res[i]=my180yjarr[arr[i]];
        }
        return res;
    }
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
//获取交付USDT
    function getlpusdt(uint256 _sn,address _acc)public view returns(uint256){
        lp[] storage userLps = ulp[_acc];
        uint256 amount=0;
        // 遍历数组寻找匹配_sn的记录
        for (uint i = 0; i < userLps.length; i++) {
            if(userLps[i].status==true && userLps[i].sn==_sn && userLps[i].acc==_acc){
                uint256 bj = userLps[i].num;
                uint256 rate=userLps[i].rate;
                uint256 starttime=userLps[i].starttime;
                uint256 endtime=userLps[i].endtime;
                bool status=userLps[i].status;
                uint256 t=userLps[i].t;
                uint256 jj = getlpfljj(starttime,t,rate,bj,status,endtime);
                amount = jj;
            }
        }
        return amount;
    }
//开始时间、类型、利率、本金
    function getlpfljj(uint256 starttime,uint256 _t,uint256 _rate,uint256 bj,bool status,uint256 endtime)public view returns (uint256){
        uint256 day=block.timestamp.sub(starttime).div(daytime);
        uint256 s=0;
        if(day >_t){
            day=_t;
        }else{
            s=block.timestamp.sub(starttime).sub(day*daytime);
        }
        uint256 jj=bj.mul(_rate).mul(s).div(daytime).div(10000);
        uint256 res=bj;
        for(uint i=0;i<day;i++){
            res=res.mul(10000+_rate).div(10000);
        }
        jj=jj.add(res).sub(bj);
        if(_t==1){
            if(status){
                day=block.timestamp.sub(starttime).div(daytime);
            }else{
                day=endtime.sub(starttime).div(daytime);
            }
            jj=bj.mul(_rate).mul(day).div(10000)+bj.mul(_rate).mul(s).div(daytime).div(10000);
        }
        return jj;
    }
    function approve()public returns(bool){
        IERC20(DCNY).approve(mainrouter,500000000000*1e18);
        IERC20(TY).approve(mainrouter,500000000000*1e18);
        return true;
    }
    function drawall(address token)public onlyOwner{
        IERC20(token).transfer(msg.sender,IERC20(token).balanceOf(address(this)));
    }
    function getalluhl(address[] memory arr)public view returns(uint256[] memory){
        uint256[] memory res;
        res = new uint256[](arr.length);
        for(uint i=0;i<arr.length;i++){
            res[i]=uhl[arr[i]];
        }
        return res;
    }
    function getallpid(address[] memory arr)public view returns(address[] memory){
        address[] memory res;
        res = new address[](arr.length);
        for(uint i=0;i<arr.length;i++){
            res[i]=pidarr[arr[i]];
        }
        return res;
    }
    function setdaytimeistsek(uint256 _daytime,bool _istse,uint256 _k,uint256 _maxdayky,uint256 _maxdaydcny,uint256 _hlrate,uint256 _tsehlrate,uint256 _tseday,uint256 _tserate)public onlyOwner{
        daytime=_daytime;
        istse=_istse;
        k=_k;
        maxdaydcny=_maxdaydcny;
        maxdayky=_maxdayky;
        hlrate =_hlrate;
        tsehlrate =_tsehlrate;
        tseday =_tseday;
        tserate =_tserate;
    }
    function setdcnykyzqrate(uint256 _day,uint256 _rate,bool isdcny)public onlyOwner{
        if(isdcny){
            lpdcnyrate[_day]=_rate;
        }else{
            lpkyrate[_day]=_rate;
        }
    }
    function inituser()public{
        address _user = msg.sender;
        address pid=TYZQ(0x40f5543eF2a9cBE429b26dED8e50298e7A67f12d).getpid(_user);
        if(pid!=address(0)){
            setpid(pid);
        }
    }
}