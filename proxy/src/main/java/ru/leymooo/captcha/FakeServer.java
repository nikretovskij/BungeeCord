package ru.leymooo.captcha;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import java.util.Random;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.protocol.packet.Chat;
import net.md_5.bungee.protocol.packet.KeepAlive;
import net.md_5.bungee.protocol.packet.Login;
import net.md_5.bungee.protocol.packet.extra.ChunkPacket;
import net.md_5.bungee.protocol.packet.extra.ConfirmTransaction;
import net.md_5.bungee.protocol.packet.extra.EntityEffect;
import net.md_5.bungee.protocol.packet.extra.PlayerAbilities;
import net.md_5.bungee.protocol.packet.extra.PlayerPositionRotation;
import net.md_5.bungee.protocol.packet.extra.SetSlot;
import net.md_5.bungee.protocol.packet.extra.SpawnPosition;

public class FakeServer
{

    @Setter
    private CaptchaConnector reciever;
    private String captchaAnswer;
    private int retries = 3;
    private static Random random = new Random();
    //========================================================================
    private static final Login loginPacket = new Login( -1, (short) 0, Configuration.getInstance().getWorldType(), (short) 0, (short) 100, "flat", false );
    private static final SpawnPosition spawnPositionPacket = new SpawnPosition( 5, 60, 5 );
    private static final SetSlot setSlotPacket = new SetSlot( 0, 36, 358, 0 );
    private static final ChunkPacket chunkPacket = new ChunkPacket( 0, 0, new byte[ 256 ] );
    private static final PlayerAbilities abilitiesPacket = new PlayerAbilities( (byte) 6, 0.0F, 0.0F );
    private static final EntityEffect entityEffectPacket = new EntityEffect( -1, (byte) 16, (byte) 2, 30 * 20, (byte) 0x01 );
    //========================================================================
    @Getter
    private final ConfirmTransaction transactionPacket = new ConfirmTransaction( (byte) 0, (short) random.nextInt( Short.MAX_VALUE ), false );
    @Getter
    private final KeepAlive keepAlivePacket = new KeepAlive( random.nextInt( 9999 ) );
    @Getter
    private final PlayerPositionRotation playerPositionPacket = new PlayerPositionRotation( 5.0D, 50.0D, 5.0D, 90.0F, 54.2F, random.nextInt( 9999 ), false );
    //========================================================================

    public FakeServer(CaptchaConnector pr)
    {
        this.reciever = pr;
    }

    public void sendJoinPackets()
    {

        UserConnection con = this.reciever.getUserConnection();
        Connection.Unsafe unsafe = con.unsafe();
        con.setClientEntityId( -1 );

        unsafe.sendPacket( loginPacket );
        this.reciever.debugPlayer( "Sended: ".concat( loginPacket.toString() ), false, false );

        unsafe.sendPacket( spawnPositionPacket );
        this.reciever.debugPlayer( "Sended: ".concat( spawnPositionPacket.toString() ), false, false );

        unsafe.sendPacket( abilitiesPacket );
        this.reciever.debugPlayer( "Sended: ".concat( abilitiesPacket.toString() ), false, false );

        unsafe.sendPacket( playerPositionPacket );
        this.reciever.debugPlayer( "Sended: ".concat( playerPositionPacket.toString() ), false, false );

        unsafe.sendPacket( chunkPacket );
        this.reciever.debugPlayer( "Sended: ".concat( chunkPacket.toString() ), false, false );

        unsafe.sendPacket( setSlotPacket );
        this.reciever.debugPlayer( "Sended: ".concat( setSlotPacket.toString() ), false, false );

        unsafe.sendPacket( keepAlivePacket );
        this.reciever.debugPlayer( "Sended: ".concat( keepAlivePacket.toString() ), false, false );

        unsafe.sendPacket( transactionPacket );
        this.reciever.debugPlayer( "Sended: ".concat( transactionPacket.toString() ), false, false );

        unsafe.sendPacket( entityEffectPacket );
        this.reciever.debugPlayer( "Sended: ".concat( entityEffectPacket.toString() ), false, false );

        if ( con.getPendingConnection().getHandshake().getProtocolVersion() <= 47 )
        {
            this.reciever.setTpconfirm( true );
        }

        this.getAndSendCaptcha();
    }

    public void getAndSendCaptcha()
    {

        Object[] captcha = CaptchaGenerator.getInstance().getCaptchaAnswerWithPacket( this.reciever.getUserConnection().getPendingConnection().getHandshake().getProtocolVersion() );
        Channel channel = this.reciever.getUserConnection().getCh().getHandle();
        this.captchaAnswer = (String) captcha[0];
        channel.write( ( (ByteBuf) captcha[1] ).copy() );
        channel.flush();
        this.reciever.debugPlayer( "Sended capthca map packet. Answer is ".concat( this.captchaAnswer ), false, false );

    }

    public void captchaEnter(Chat chat)
    {
        String msg = chat.getMessage().replace( "/", "" );
        if ( this.reciever.isBot() || msg.length() >= 5 || !this.captchaAnswer.equalsIgnoreCase( msg ) )
        {
            if ( --this.retries == 0 )
            {
                kick( this.reciever.isBot() ? Configuration.getInstance().getBotKick() : Configuration.getInstance().getWrongCaptchaKick() );
                return;
            }
            this.reciever.getUserConnection().sendMessage( String.format( Configuration.getInstance().getWrongCaptcha(), this.retries, this.retries == 1 ? "а" : "и" ) );
            this.getAndSendCaptcha();
            return;
        }
        this.reciever.finish();
    }

    public void enterCapthca()
    {
        this.reciever.getUserConnection().sendMessage( Configuration.getInstance().getEnterCaptcha() );
    }

    public void kick(String reason)
    {
        this.reciever.debugPlayer( "Disconnected with: ".concat( reason ), false, false );
        this.reciever.getUserConnection().disconnect( reason );
    }
}
