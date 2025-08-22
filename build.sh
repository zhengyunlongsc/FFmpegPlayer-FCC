#!/bin/bash

export NDKROOT=/home/ndk/android-ndk-r14b
export TARGET=$1

function build
{
	echo -e "\033[32m build start \033[0m"
	./configure \
	--prefix=$PREFIX \
	--disable-programs \
	--disable-encoders \
	--disable-muxers \
	--disable-filters \
	--disable-doc \
	--disable-static \
	--disable-ffmpeg \
	--disable-ffplay \
	--disable-ffprobe \
	--disable-avdevice \
	--disable-symver \
	--disable-x86asm \
	--disable-armv5te \
	--disable-armv6 \
	--disable-armv6t2 \
	--disable-asm \
	--enable-cross-compile \
	--enable-neon \
	--enable-hwaccels \
	--enable-gpl \
	--enable-version3 \
	--enable-postproc \
	--enable-shared \
	--enable-jni \
	--enable-mediacodec \
	--enable-decoder=h264_mediacodec \
	--enable-protocols \
	--enable-protocol=rtsp \
	--enable-protocol=https \
	--enable-nonfree \
	--enable-parser=h264 \
	--enable-decoder=h264 \
	--enable-muxer=h264 \
	--enable-demuxer=h264 \
	--cc=$CC \
	--cross-prefix=$CROSS_PREFIX \
	--sysroot=$NDKROOT/platforms/android-$ANDROID_API/arch-$ARCH \
	--extra-cflags="$FLAGS" \
	--extra-ldflags="$ADDI_LDFLAGS" \
	--arch=$CPU \
	--target-os=android
	
	sudo make clean
	sudo make -j8
	sudo make install
	echo -e "\033[32m build successful\033[0m"
}

function arm
{
	#arm
	echo "Start build for arm for ABI"
	CPU='arm'
	ABI='armeabi-v7a'
	ARCH='arm'
	ANDROID='androideabi'
	NATIVE_CPU='armeabi-v7a'
	ANDROID_API=17

	TOOLCHAIN=$NDKROOT/toolchains/$CPU-linux-$ANDROID-4.9/prebuilt/linux-x86_64
	CC=$TOOLCHAIN/bin/$CPU-linux-$ANDROID-gcc
	CROSS_PREFIX=$TOOLCHAIN/bin/$CPU-linux-$ANDROID-
	ISYSROOT=$NDKROOT/sysroot
	ASM=$ISYSROOT/usr/include/$CPU-linux-$ANDROID
	ADDI_CFLAGS=" -marm"
	FLAGS=" -I$ASM -isysroot $ISYSROOT -D__ANDROID_API__=$ANDROID_API -U_FILE_OFFSET_BITS -Os -fPIC -DANDROID -Wno-deprecated -mfloat-abi=softfp -marm"
	PREFIX=/home/d/FFmpeg/android/$NATIVE_CPU

	echo "Biuld param={CPU=$CPU,ABP=$ABI,ARCH=$ARCH,ANDROID=$ANDROID,NATIVE_CPU=$NATIVE_CPU}"
	echo "NDKROOT=$NDKROOT,TOOLCHAIN=$TOOLCHAIN"
	build
}

function arm64
{
	#arm64
	echo "Start build for aarch64 for ABI"
	CPU='aarch64'
	ABI='arm64-v8a'
	ARCH='arm64'
	ANDROID='android'
	NATIVE_CPU='arm64-v8a'
	ANDROID_API=21

	TOOLCHAIN=$NDKROOT/toolchains/$CPU-linux-$ANDROID-4.9/prebuilt/linux-x86_64
	CC=$TOOLCHAIN/bin/$CPU-linux-$ANDROID-gcc
	CROSS_PREFIX=$TOOLCHAIN/bin/$CPU-linux-$ANDROID-
	ISYSROOT=$NDKROOT/sysroot
	ASM=$ISYSROOT/usr/include/$CPU-linux-$ANDROID
	ADDI_CFLAGS=" -marm"
	FLAGS=" -I$ASM -isysroot $ISYSROOT -D__ANDROID_API__=$ANDROID_API -U_FILE_OFFSET_BITS -Os -fPIC -DANDROID -Wno-deprecated"
	PREFIX=/home/d/FFmpeg/android/$NATIVE_CPU

	echo "Biuld param={CPU=$CPU,ABP=$ABI,ARCH=$ARCH,ANDROID=$ANDROID,NATIVE_CPU=$NATIVE_CPU}"
	echo "NDKROOT=$NDKROOT,TOOLCHAIN=$TOOLCHAIN"
	build
}

function x86
{
	#x86
	echo "Start build for x86 for ABI"
	CPU='x86'
	ABI='x86'
	ARCH='x86'
	ANDROID='android'
	NATIVE_CPU='x86'
	ANDROID_API=17

	TOOLCHAIN=$NDKROOT/toolchains/$CPU-4.9/prebuilt/linux-x86_64
	CC=$TOOLCHAIN/bin/i686-linux-$ANDROID-gcc
	CROSS_PREFIX=$TOOLCHAIN/bin/i686-linux-$ANDROID-
	ISYSROOT=$NDKROOT/sysroot
	ASM=$ISYSROOT/usr/include/i686-linux-$ANDROID
	ADDI_CFLAGS=" -marm"
	FLAGS="-fomit-frame-pointer -Os -fPIC"
	PREFIX=/home/d/FFmpeg/android/$NATIVE_CPU

	echo "Biuld param={CPU=$CPU,ABP=$ABI,ARCH=$ARCH,ANDROID=$ANDROID,NATIVE_CPU=$NATIVE_CPU}"
	echo "NDKROOT=$NDKROOT,TOOLCHAIN=$TOOLCHAIN"
	build
}

function x86_64
{
	#x86_64
	echo "Start build for x86_64 for ABI"
	CPU='x86_64'
	ABI='x86_64'
	ARCH='x86_64'
	ANDROID='android'
	NATIVE_CPU='x86_64'
	ANDROID_API=21
	
	TOOLCHAIN=$NDKROOT/toolchains/$CPU-4.9/prebuilt/linux-x86_64
	CC=$TOOLCHAIN/bin/$CPU-linux-$ANDROID-gcc
	CROSS_PREFIX=$TOOLCHAIN/bin/$CPU-linux-$ANDROID-
	ISYSROOT=$NDKROOT/sysroot
	ASM=$ISYSROOT/usr/include/$CPU-linux-$ANDROID
	ADDI_CFLAGS=" -marm"
	FLAGS="-fomit-frame-pointer -fPIC"
	PREFIX=/home/d/FFmpeg/android/$NATIVE_CPU

	echo "Biuld param={CPU=$CPU,ABP=$ABI,ARCH=$ARCH,ANDROID=$ANDROID,NATIVE_CPU=$NATIVE_CPU}"
	echo "NDKROOT=$NDKROOT,TOOLCHAIN=$TOOLCHAIN"
	build
}

if [ "$TARGET" == "arm" ];then 
	echo "---build arm --->"
	arm
elif [ "$TARGET" == "arm64" ];then
	echo "---build arm64 --->"
	arm64
elif [ "$TARGET" == "x86" ];then
	echo "---build x86 --->"
	x86	
elif [ "$TARGET" == "x86_64" ];then
	echo "---build x86_64 --->"
	x86_64
else
	echo "---build all --->"
	arm
	arm64
	x86
	x86_64
fi

#2024年7月25日18:45:24 上面shell执行编译x86可运行

